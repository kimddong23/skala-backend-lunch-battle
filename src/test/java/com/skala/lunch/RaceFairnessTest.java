package com.skala.lunch;

import com.skala.lunch.dto.BattleDto;
import com.skala.lunch.dto.RaceDto;
import com.skala.lunch.dto.VoteRequestDto;
import com.skala.lunch.repository.*;
import com.skala.lunch.service.BattleService;
import com.skala.lunch.service.RaceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 레이스가 정말 무작위인지, 그리고 화면에 올릴 만한 경기인지 확인한다.
 *
 * "랜덤이다" 는 말로 주장할 수 없다. 여러 판을 돌려 분포를 봐야 한다.
 */
@SpringBootTest
@Transactional
@DisplayName("레이스 공정성")
class RaceFairnessTest {

    private static final int ROUNDS = 200;

    @Autowired RaceService raceService;
    @Autowired BattleService battleService;
    @Autowired BattleRepository battleRepository;
    @Autowired CandidateRepository candidateRepository;
    @Autowired RaceRepository raceRepository;

    /** 후보 5개짜리 배틀을 만들고, 1번 후보에만 표를 몰아준다. */
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
            // 첫 후보에 8표 몰아주기
            for (long m = 1; m <= 8; m++) {
                battleService.vote(b.getId(), VoteRequestDto.builder()
                        .memberId(m).candidateId(candIds.get(0)).build());
            }
        }
        return b.getId();
    }

    @Test
    @DisplayName("200판 — 우승이 한쪽으로 쏠리지 않는다")
    void 우승_분포() {
        Map<String, Integer> wins = new LinkedHashMap<>();
        int maxTicks = 0, minTicks = Integer.MAX_VALUE, unfinished = 0;

        for (int i = 0; i < ROUNDS; i++) {
            RaceDto race = raceService.run(freshBattle(i, false));
            wins.merge(race.getWinnerName(), 1, Integer::sum);
            maxTicks = Math.max(maxTicks, race.getTotalTicks());
            minTicks = Math.min(minTicks, race.getTotalTicks());
            unfinished += race.getLanes().stream()
                    .filter(l -> l.getFinishTick() == null).toList().size();
        }

        System.out.println("── 우승 분포 (후보 5, 표 없음) ──");
        wins.forEach((k, v) -> System.out.printf("   %-14s %3d판 (%.1f%%)%n", k, v, v * 100.0 / ROUNDS));
        System.out.printf("   경기 길이 %d~%d틱 · 미완주 %d마리%n", minTicks, maxTicks, unfinished);

        // 5마리이므로 무작위라면 각 20% 근처. 한 마리가 절반을 넘으면 무작위가 아니다.
        int top = wins.values().stream().max(Integer::compare).orElse(0);
        assertThat(wins.size()).as("우승이 여러 메뉴에 퍼짐").isGreaterThanOrEqualTo(4);
        assertThat(top).as("한 메뉴가 절반 넘게 이기지 않음").isLessThan(ROUNDS / 2);

        // 화면에 올릴 경기 — 아무도 트랙 위에 갇히면 안 된다
        assertThat(unfinished).as("전원 완주").isZero();
        // 화면은 틱당 한 프레임으로 재생한다. 400틱이면 60fps 에서 약 6.7초 —
        // 결과를 기다릴 만하면서 지루하지 않은 길이다.
        assertThat(maxTicks).as("애니메이션이 지나치게 길지 않음")
                .isLessThanOrEqualTo(com.skala.lunch.entity.Race.MAX_TICKS);
    }

    @Test
    @DisplayName("표를 몰아줘도 뒤집힌다 — 응원은 거들 뿐")
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
        System.out.printf("── 표 몰빵 후보의 우승률: %.1f%% (무작위라면 20%%)%n", rate);

        // 응원이 유리하긴 해야 하지만, 결과를 좌우하면 레이스를 할 이유가 없다
        assertThat(rate).as("응원 효과가 있음").isGreaterThan(20.0);
        assertThat(rate).as("응원만으로 결정되지 않음").isLessThan(60.0);
    }

    @Test
    @DisplayName("같은 시드면 같은 경기가 재현된다")
    void 재현성() {
        Long battleId = freshBattle(2000, true);
        RaceDto first = raceService.run(battleId);
        RaceDto again = raceService.replay(battleId);

        assertThat(again.getSeed()).isEqualTo(first.getSeed());
        assertThat(again.getWinnerName()).isEqualTo(first.getWinnerName());
        assertThat(again.getTotalTicks()).isEqualTo(first.getTotalTicks());

        Map<Long, List<Integer>> firstTracks = new HashMap<>();
        first.getLanes().forEach(l -> firstTracks.put(l.getCandidateId(), l.getTrack()));
        again.getLanes().forEach(l ->
                assertThat(l.getTrack()).as("주행 기록까지 동일")
                        .isEqualTo(firstTracks.get(l.getCandidateId())));
    }

    @Test
    @DisplayName("후보가 하나뿐이면 레이스를 할 수 없고, 두 번 달릴 수도 없다")
    void 레이스_제약() {
        LocalDate date = LocalDate.now().plusDays(3000);
        BattleDto b = battleService.openBattle(BattleDto.builder()
                .battleDate(date).closesAt(LocalDateTime.of(date, LocalTime.of(23, 59))).build());
        battleService.addCandidate(b.getId(), 1L, 1L);

        assertThat(catchThrowable(() -> raceService.run(b.getId())))
                .hasMessageContaining("둘 이상");

        battleService.addCandidate(b.getId(), 3L, 1L);
        raceService.run(b.getId());
        assertThat(catchThrowable(() -> raceService.run(b.getId())))
                .hasMessageContaining("이미 레이스가 끝난");
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
