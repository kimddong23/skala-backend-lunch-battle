package com.skala.lunch;

import com.skala.lunch.dto.BattleDto;
import com.skala.lunch.dto.RaceDto;
import com.skala.lunch.dto.VoteRequestDto;
import com.skala.lunch.entity.Battle;
import com.skala.lunch.repository.*;
import com.skala.lunch.service.BattleService;
import com.skala.lunch.service.RaceService;
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
 * 경주가 정말 무작위인지, 그리고 <b>응원과 감점이 실제로 작동하는지</b> 확인한다.
 *
 * 뒤쪽이 더 중요하다. 결과가 순전히 운으로 갈리면 투표 화면도 감점 규칙도
 * 있으나 마나 한 장식이 된다. 그래서 "표를 받은 쪽이 실제로 길을 덜 헤매는가",
 * "최근에 이긴 쪽이 실제로 불리한가" 를 숫자로 확인한다.
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
    @Autowired RestaurantRepository restaurantRepository;

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
    @DisplayName("응원은 유리하게 만들지만 결과를 정하지는 못한다")
    void 응원은_거들_뿐() {
        int favoriteWins = 0;

        for (int i = 0; i < ROUNDS; i++) {
            Long battleId = freshBattle(1000 + i, true);
            String favorite = candidateRepository
                    .findByBattleIdOrderByVoteCountDescIdAsc(battleId)
                    .get(0).getRestaurant().getName();
            RaceDto race = raceService.run(battleId);
            if (race.getWinnerName().equals(favorite)) {
                favoriteWins++;
            }
        }

        double rate = favoriteWins * 100.0 / ROUNDS;
        System.out.printf("── 표를 몰아준 후보의 우승률: %.1f%% (아무 영향 없다면 20%%)%n", rate);

        assertThat(rate).as("응원에 효과가 있음").isGreaterThan(20.0);
        assertThat(rate).as("응원만으로 결정되지 않음").isLessThan(60.0);
    }

    @Test
    @DisplayName("응원은 판단력으로 작동한다 — 표를 받은 햄스터가 덜 헤맨다")
    void 응원은_길을_알려준다() {
        double cheeredSum = 0, plainSum = 0;
        int cheeredN = 0, plainN = 0;

        for (int i = 0; i < ROUNDS; i++) {
            RaceDto race = raceService.run(freshBattle(2000 + i, true));
            for (RaceDto.LaneDto lane : race.getLanes()) {
                if (lane.getCheerBonus() > 0) {
                    cheeredSum += lane.getEfficiency();
                    cheeredN++;
                } else {
                    plainSum += lane.getEfficiency();
                    plainN++;
                }
            }
        }

        double cheered = cheeredSum / cheeredN, plain = plainSum / plainN;
        System.out.printf("── 최단 경로 대비 효율 ──%n   응원 받음 %.1f%%  ·  응원 없음 %.1f%%%n", cheered, plain);

        // 효율 = 최단 걸음 / 실제 걸음. 판단력이 높으면 덜 돌아가므로 값이 커진다.
        assertThat(cheered)
                .as("응원이 판단력을 올려 실제로 길을 덜 헤매게 한다")
                .isGreaterThan(plain);
    }

    @Test
    @DisplayName("최근에 이긴 메뉴는 실제로 불리하다 — 감점이 장식이 아니다")
    void 감점은_실제로_불리하다() {
        // 초기 데이터가 이미 최근 우승 이력을 깔아 둔다 (식당 4·7).
        // 새로 심으면 하루 1배틀 제약과 부딪히므로 있는 이력을 그대로 쓴다.
        String loadedName = restaurantRepository.findById(4L).orElseThrow().getName();

        int loadedWins = 0;
        double loadedTicks = 0, otherTicks = 0;
        int loadedN = 0, otherN = 0;

        for (int i = 0; i < ROUNDS; i++) {
            RaceDto race = raceService.run(freshBattle(3000 + i, false));
            for (RaceDto.LaneDto lane : race.getLanes()) {
                boolean loaded = lane.getRestaurantName().equals(loadedName);
                if (loaded) {
                    assertThat(lane.getHandicap()).as("짐이 실려 있어야 한다").isGreaterThan(0.0);
                    loadedTicks += lane.getFinishTick();
                    loadedN++;
                    if (lane.getRank() == 1) loadedWins++;
                } else if (lane.getHandicap() == 0.0) {
                    // 짐을 진 다른 식당(7번)은 비교군에서 뺀다
                    otherTicks += lane.getFinishTick();
                    otherN++;
                }
            }
        }

        double loadedAvg = loadedTicks / loadedN, otherAvg = otherTicks / otherN;
        double winRate = loadedWins * 100.0 / ROUNDS;
        System.out.printf("── 최근 우승자(%s) ──%n", loadedName);
        System.out.printf("   탈출까지 평균 %.1f걸음 (짐 없는 쪽 %.1f걸음)%n", loadedAvg, otherAvg);
        System.out.printf("   우승률 %.1f%% (짐이 없다면 20%%)%n", winRate);

        assertThat(loadedAvg).as("짐을 진 쪽이 더 오래 걸린다").isGreaterThan(otherAvg);
        assertThat(winRate).as("짐을 진 쪽의 우승률이 낮아진다").isLessThan(20.0);
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
