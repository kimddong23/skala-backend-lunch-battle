package com.skala.lunch.race;

import com.skala.lunch.battle.BattleDto;
import com.skala.lunch.race.RaceDto;
import com.skala.lunch.battle.VoteRequestDto;
import com.skala.lunch.battle.Battle;
import com.skala.lunch.battle.BattleRepository;
import com.skala.lunch.battle.CandidateRepository;
import com.skala.lunch.battle.BattleService;
import com.skala.lunch.race.RaceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 경주가 정말 무작위이고, 화면에 올릴 만한 경기인지 확인한다.
 *
 * 득표가 결과를 바꾸지 않는지는 RaceIsFairTest 에서 따로 본다.
 */
@SpringBootTest
@Transactional
@DisplayName("경주 공정성")
class RaceFairnessTest {

    private static final int ROUNDS = 150;

    @Autowired RaceService raceService;
    @Autowired BattleService battleService;
    @Autowired BattleRepository battleRepository;
    @Autowired CandidateRepository candidateRepository;

    /** 후보 5개짜리 배틀. withVotes 면 첫 후보에만 표를 몰아준다. */
    private Long freshBattle(int index, boolean withVotes) {
        LocalDate date = LocalDate.now().plusDays(100 + index);
        BattleDto b = battleService.openBattle(BattleDto.builder()
                .battleDate(date)
                .closesAt(LocalDateTime.of(date, LocalTime.of(23, 59)))
                .build());

        List<Long> candIds = new ArrayList<>();
        for (long r : new long[]{1, 3, 4, 7, 9}) {
            candIds.add(battleService.addCandidate(b.getId(), r, 1L).getId());
        }
        if (withVotes) {
            for (long m = 1; m <= 8; m++) {
                battleService.vote(b.getId(), VoteRequestDto.builder()
                        .memberId(m).candidateId(candIds.get(0)).build());
            }
        }
        return b.getId();
    }

    @Test
    @DisplayName("150판 — 우승이 한쪽으로 쏠리지 않고 전원 탈출한다")
    void 우승_분포() {
        Map<String, Integer> wins = new LinkedHashMap<>();
        int maxTicks = 0, minTicks = Integer.MAX_VALUE, stuck = 0;

        for (int i = 0; i < ROUNDS; i++) {
            RaceDto race = raceService.run(freshBattle(i, false));
            wins.merge(race.getWinnerName(), 1, Integer::sum);
            maxTicks = Math.max(maxTicks, race.getTotalTicks());
            minTicks = Math.min(minTicks, race.getTotalTicks());
            stuck += race.getLanes().stream().filter(l -> l.getFinishTick() == null).count();
        }

        System.out.println("── 우승 분포 (후보 5, 표 없음) ──");
        wins.forEach((k, v) -> System.out.printf("   %-14s %3d판 (%.1f%%)%n", k, v, v * 100.0 / ROUNDS));
        System.out.printf("   경기 길이 %d~%d걸음 · 못 빠져나온 햄스터 %d마리%n", minTicks, maxTicks, stuck);

        int top = wins.values().stream().max(Integer::compare).orElse(0);
        assertThat(wins.size()).as("우승이 여러 메뉴에 퍼짐").isGreaterThanOrEqualTo(4);
        assertThat(top).as("한 메뉴가 절반 넘게 이기지 않음").isLessThan(ROUNDS / 2);

        // 미로에 갇힌 햄스터는 화면에서 고장으로 보인다
        assertThat(stuck).as("전원 출구를 찾음").isZero();
        assertThat(maxTicks).as("경기가 지나치게 길지 않음").isLessThanOrEqualTo(RaceService.MAX_TICKS);
    }




    @Test
    @DisplayName("같은 시드면 미로도 주행도 그대로 재현된다")
    void 재현성() {
        Long battleId = freshBattle(4000, true);
        RaceDto first = raceService.run(battleId);
        RaceDto again = raceService.replay(battleId);

        assertThat(again.getSeed()).isEqualTo(first.getSeed());
        assertThat(again.getWalls()).as("미로가 같아야 한다").isEqualTo(first.getWalls());
        assertThat(again.getOptimalPath()).isEqualTo(first.getOptimalPath());
        assertThat(again.getWinnerName()).isEqualTo(first.getWinnerName());
        assertThat(again.getTotalTicks()).isEqualTo(first.getTotalTicks());

        Map<Long, List<Integer>> firstPaths = new HashMap<>();
        first.getLanes().forEach(l -> firstPaths.put(l.getCandidateId(), l.getPath()));
        again.getLanes().forEach(l ->
                assertThat(l.getPath()).as("걸음 하나까지 동일")
                        .isEqualTo(firstPaths.get(l.getCandidateId())));
    }

    @Test
    @DisplayName("아무도 최단 경로보다 적게 걸을 수 없다")
    void 최단경로가_진짜_최단이다() {
        for (int i = 0; i < 40; i++) {
            RaceDto race = raceService.run(freshBattle(5000 + i, false));
            for (RaceDto.LaneDto lane : race.getLanes()) {
                assertThat(lane.getSteps())
                        .as("최단 %d칸보다 적게 걸은 햄스터가 있으면 BFS 가 최단을 못 찾은 것"
                                .formatted(race.getOptimalLength()))
                        .isGreaterThanOrEqualTo(race.getOptimalLength());
            }
            assertThat(race.getLanes().stream().mapToDouble(RaceDto.LaneDto::getEfficiency).max().orElse(0))
                    .as("효율은 100%를 넘을 수 없다").isLessThanOrEqualTo(100.0);
        }
    }

    @Test
    @DisplayName("후보가 하나뿐이면 달릴 수 없고, 두 번 달릴 수도 없다")
    void 경주_제약() {
        LocalDate date = LocalDate.now().plusDays(6000);
        BattleDto b = battleService.openBattle(BattleDto.builder()
                .battleDate(date).closesAt(LocalDateTime.of(date, LocalTime.of(23, 59))).build());
        battleService.addCandidate(b.getId(), 1L, 1L);

        assertThat(catchThrowable(() -> raceService.run(b.getId())))
                .hasMessageContaining("둘 이상");

        battleService.addCandidate(b.getId(), 3L, 1L);
        raceService.run(b.getId());
        assertThat(catchThrowable(() -> raceService.run(b.getId())))
                .hasMessageContaining("이미 경주가 끝난");
    }

    @Test
    @DisplayName("다시 하기 — 초기화하면 표는 남고 새 미로에서 다시 달릴 수 있다")
    void 다시_하기() {
        Long battleId = freshBattle(7000, true);
        RaceDto first = raceService.run(battleId);
        long votesBefore = battleService.getBattle(battleId).getTotalVotes();

        raceService.reset(battleId);

        BattleDto after = battleService.getBattle(battleId);
        assertThat(after.getStatus()).as("배틀이 다시 열린다").isEqualTo(Battle.Status.OPEN);
        assertThat(after.getWinnerName()).as("우승 기록도 지워진다").isNull();
        assertThat(after.getTotalVotes()).as("표는 그대로 남는다").isEqualTo(votesBefore);
        assertThat(after.getCandidates()).as("후보도 그대로 남는다").hasSize(5);

        RaceDto second = raceService.run(battleId);
        assertThat(second.getSeed()).as("새 시드로 새 미로가 나온다").isNotEqualTo(first.getSeed());

        assertThat(catchThrowable(() -> raceService.reset(9_999_999L)))
                .as("없는 배틀은 초기화할 수 없다").isNotNull();
    }

    private Throwable catchThrowable(Runnable r) {
        try {
            r.run();
            return null;
        } catch (Throwable t) {
            return t;
        }
    }
}
