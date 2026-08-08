package com.skala.lunch;

import com.skala.lunch.battle.BattleDto;
import com.skala.lunch.race.RaceDto;
import com.skala.lunch.battle.VoteRequestDto;
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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 경주가 득표에 휘둘리지 않는지 확인한다.
 *
 * "응원은 결과를 바꾸지 않는다" 는 화면에 적어 둔 문구이자 이 서비스의 규칙이다.
 * 문구는 코드가 바뀌어도 그대로 남으므로, 실제로 그런지는 돌려 보고 세어야 한다.
 * 한쪽에 표를 몰아준 뒤 그 후보의 우승률이 1/후보수 근처에 머무는지 본다.
 */
@SpringBootTest
@Transactional
@DisplayName("경주 공정성 (득표 무관)")
class RaceIsFairTest {

    private static final int ROUNDS = 200;
    private static final int RUNNERS = 5;

    @Autowired RaceService raceService;
    @Autowired BattleService battleService;

    /** 후보 5팀. 첫 후보에만 표를 전부 몰아준다. */
    private Long battleWithLandslide(int index) {
        LocalDate date = LocalDate.now().plusDays(20_000 + index);
        BattleDto b = battleService.openBattle(BattleDto.builder()
                .battleDate(date)
                .closesAt(LocalDateTime.of(date, LocalTime.of(23, 59)))
                .build());

        List<Long> candIds = new ArrayList<>();
        for (long r : new long[]{1, 3, 4, 7, 9}) {
            candIds.add(battleService.addCandidate(b.getId(), r, 1L).getId());
        }
        for (long m = 1; m <= 10; m++) {          // 10표 전부 첫 후보에게
            battleService.vote(b.getId(), VoteRequestDto.builder()
                    .memberId(m).candidateId(candIds.get(0)).build());
        }
        return b.getId();
    }

    @Test
    @DisplayName("표를 전부 몰아줘도 우승률이 오르지 않는다")
    void 득표는_우승률을_바꾸지_않는다() {
        int favoriteWins = 0;

        for (int i = 0; i < ROUNDS; i++) {
            Long battleId = battleWithLandslide(i);
            String favorite = battleService.getBattle(battleId).getCandidates().get(0).getRestaurantName();
            RaceDto race = raceService.run(battleId);
            if (race.getWinnerName().equals(favorite)) {
                favoriteWins++;
            }
        }

        double rate = favoriteWins * 100.0 / ROUNDS;
        double expected = 100.0 / RUNNERS;                       // 20%
        // 이항분포 표준편차. 200판이면 약 2.8%p 이므로 3배(≈8.5%p)를 허용한다.
        double sigma = Math.sqrt(expected * (100 - expected) / ROUNDS);
        double band = 3 * sigma;

        System.out.printf("── 표를 전부 몰아준 후보의 우승률: %.1f%% (기대 %.0f%% · 허용 ±%.1f%%p)%n",
                rate, expected, band);

        assertThat(rate)
                .as("득표가 결과에 영향을 준다면 이 값이 기대치에서 벗어난다")
                .isCloseTo(expected, org.assertj.core.data.Offset.offset(band));
    }

    @Test
    @DisplayName("표를 받은 쪽과 못 받은 쪽의 길찾기 효율이 다르지 않다")
    void 득표는_길찾기를_돕지_않는다() {
        double favSum = 0, restSum = 0;
        int favN = 0, restN = 0;

        for (int i = 0; i < ROUNDS; i++) {
            Long battleId = battleWithLandslide(10_000 + i);
            String favorite = battleService.getBattle(battleId).getCandidates().get(0).getRestaurantName();
            RaceDto race = raceService.run(battleId);

            for (RaceDto.LaneDto lane : race.getLanes()) {
                if (lane.getRestaurantName().equals(favorite)) {
                    favSum += lane.getEfficiency(); favN++;
                } else {
                    restSum += lane.getEfficiency(); restN++;
                }
            }
        }

        double fav = favSum / favN, rest = restSum / restN;
        System.out.printf("── 최단 경로 대비 효율 ── 몰표 %.1f%% · 나머지 %.1f%%%n", fav, rest);

        assertThat(fav)
                .as("표를 받았다고 길을 더 잘 찾으면 안 된다")
                .isCloseTo(rest, org.assertj.core.data.Offset.offset(3.0));
    }

    @Test
    @DisplayName("같은 걸음에 들어와도 먼저 등록한 쪽이 늘 이기지는 않는다")
    void 동점이_등록순으로_갈리지_않는다() {
        int ties = 0, firstRegisteredWon = 0;

        for (int i = 0; i < ROUNDS * 2; i++) {
            Long battleId = battleWithLandslide(40_000 + i);
            RaceDto race = raceService.run(battleId);

            int best = race.getLanes().stream()
                    .filter(l -> l.getFinishTick() != null)
                    .mapToInt(RaceDto.LaneDto::getFinishTick).min().orElse(-1);
            List<RaceDto.LaneDto> tied = race.getLanes().stream()
                    .filter(l -> l.getFinishTick() != null && l.getFinishTick() == best)
                    .toList();
            if (tied.size() < 2) {
                continue;
            }
            ties++;
            long lowestId = tied.stream().mapToLong(RaceDto.LaneDto::getCandidateId).min().orElse(0);
            RaceDto.LaneDto won = race.getLanes().stream()
                    .filter(l -> l.getRank() == 1).findFirst().orElseThrow();
            if (won.getCandidateId() == lowestId) {
                firstRegisteredWon++;
            }
        }

        System.out.printf("── 1위 동점 %d회 · 그 중 먼저 등록한 쪽이 이긴 횟수 %d회%n",
                ties, firstRegisteredWon);

        assertThat(ties).as("동점 표본이 있어야 판단할 수 있다").isGreaterThan(5);
        // 등록 순서로 갈린다면 전부 먼저 등록한 쪽이 이긴다. 실제로 그런 상태였다.
        assertThat(firstRegisteredWon)
                .as("동점이 등록 순서로 갈리면 안 된다 (%d회 중 %d회)".formatted(ties, firstRegisteredWon))
                .isLessThan(ties);
    }

    @Test
    @DisplayName("몰표 후보의 스탯 분포가 나머지와 같다 — 어디에도 보정이 붙지 않는다")
    void 스탯에_보정이_없다() {
        double favSense = 0, restSense = 0, favPace = 0, restPace = 0;
        int favN = 0, restN = 0;

        for (int i = 0; i < ROUNDS; i++) {
            Long battleId = battleWithLandslide(20_000 + i);
            String favorite = battleService.getBattle(battleId).getCandidates().get(0).getRestaurantName();
            RaceDto race = raceService.run(battleId);

            for (RaceDto.LaneDto lane : race.getLanes()) {
                if (lane.getRestaurantName().equals(favorite)) {
                    favSense += lane.getSense(); favPace += lane.getPace(); favN++;
                } else {
                    restSense += lane.getSense(); restPace += lane.getPace(); restN++;
                }
            }
        }

        double fs = favSense / favN, rs = restSense / restN;
        double fp = favPace / favN, rp = restPace / restN;
        System.out.printf("── 평균 스탯 ── 판단력 몰표 %.3f / 나머지 %.3f · 발놀림 몰표 %.3f / 나머지 %.3f%n",
                fs, rs, fp, rp);

        // 상수를 참조하지 않는다. 같은 분포에서 뽑았다면 평균이 붙어야 한다는 사실만 본다.
        assertThat(fs).as("판단력에 득표 보정이 붙지 않았다")
                .isCloseTo(rs, org.assertj.core.data.Offset.offset(0.02));
        assertThat(fp).as("발놀림에 득표 보정이 붙지 않았다")
                .isCloseTo(rp, org.assertj.core.data.Offset.offset(0.02));
    }
}
