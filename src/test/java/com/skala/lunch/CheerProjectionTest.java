package com.skala.lunch;

import com.skala.lunch.dto.BattleDto;
import com.skala.lunch.dto.CandidateDto;
import com.skala.lunch.dto.RaceDto;
import com.skala.lunch.dto.VoteRequestDto;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 출전 명단이 보여 주는 "판단력 +0.0X" 가 경주에서 실제로 적용되는 값과 같은지 확인한다.
 *
 * 화면은 투표하기 전에 "이 표가 무엇을 하는지" 를 미리 알려 준다. 그 숫자가
 * 실제와 다르면 화면이 거짓말을 하는 셈이고, 응원이 의미 있다는 설명 자체가 무너진다.
 * 두 곳에서 따로 계산하므로 한쪽만 바뀌어도 조용히 어긋난다 — 그래서 묶어 둔다.
 */
@SpringBootTest
@Transactional
@DisplayName("응원 가산 표기")
class CheerProjectionTest {

    @Autowired BattleService battleService;
    @Autowired RaceService raceService;

    @Test
    @DisplayName("명단에 적힌 예상 판단력이 경주에 그대로 적용된다")
    void 표기와_실제가_같다() {
        LocalDate date = LocalDate.now().plusDays(8100);
        BattleDto battle = battleService.openBattle(BattleDto.builder()
                .battleDate(date)
                .closesAt(LocalDateTime.of(date, LocalTime.of(23, 59)))
                .build());

        List<Long> candIds = new ArrayList<>();
        for (long r : new long[]{1, 3, 4}) {
            candIds.add(battleService.addCandidate(battle.getId(), r, 1L).getId());
        }

        // 표를 고르지 않게 나눠 준다 (5표 / 2표 / 1표)
        long member = 1;
        for (int i = 0; i < 5; i++) {
            battleService.vote(battle.getId(), VoteRequestDto.builder()
                    .memberId(member++).candidateId(candIds.get(0)).build());
        }
        for (int i = 0; i < 2; i++) {
            battleService.vote(battle.getId(), VoteRequestDto.builder()
                    .memberId(member++).candidateId(candIds.get(1)).build());
        }
        battleService.vote(battle.getId(), VoteRequestDto.builder()
                .memberId(member++).candidateId(candIds.get(2)).build());

        Map<String, Double> shown = battleService.getBattle(battle.getId()).getCandidates().stream()
                .collect(Collectors.toMap(CandidateDto::getRestaurantName, CandidateDto::getCheerBonus));

        RaceDto race = raceService.run(battle.getId());
        Map<String, Double> applied = race.getLanes().stream()
                .collect(Collectors.toMap(RaceDto.LaneDto::getRestaurantName,
                        RaceDto.LaneDto::getCheerBonus));

        System.out.println("── 명단 표기 vs 경주 적용 ──");
        shown.forEach((name, v) ->
                System.out.printf("   %-14s 표기 +%.2f · 적용 +%.2f%n", name, v, applied.get(name)));

        assertThat(applied)
                .as("명단에 적어 둔 값과 경주가 쓴 값이 같아야 한다")
                .containsExactlyInAnyOrderEntriesOf(shown);
    }

    @Test
    @DisplayName("가산 합계가 최대치를 넘지 않는다")
    void 가산_상한() {
        LocalDate date = LocalDate.now().plusDays(8200);
        BattleDto battle = battleService.openBattle(BattleDto.builder()
                .battleDate(date)
                .closesAt(LocalDateTime.of(date, LocalTime.of(23, 59)))
                .build());

        List<Long> candIds = new ArrayList<>();
        for (long r : new long[]{1, 3, 4, 7}) {
            candIds.add(battleService.addCandidate(battle.getId(), r, 1L).getId());
        }
        for (long m = 1; m <= 8; m++) {
            battleService.vote(battle.getId(), VoteRequestDto.builder()
                    .memberId(m).candidateId(candIds.get((int) (m % candIds.size()))).build());
        }

        List<CandidateDto> cands = battleService.getBattle(battle.getId()).getCandidates();
        double sum = cands.stream().mapToDouble(CandidateDto::getCheerBonus).sum();

        // 후보마다 소수 둘째 자리에서 반올림하므로 한 명당 최대 0.005 가 어긋난다.
        // 고정값으로 두면 후보가 늘었을 때 이 테스트가 이유 없이 깨진다.
        double tolerance = 0.005 * cands.size();
        System.out.printf("── 가산 합계 %.3f (최대 %.2f · 후보 %d명 · 허용 오차 %.3f)%n",
                sum, RaceService.MAX_CHEER, cands.size(), tolerance);

        // 표를 나눠 가지므로 합계는 최대치 하나만큼이다
        assertThat(sum).isCloseTo(RaceService.MAX_CHEER,
                org.assertj.core.data.Offset.offset(tolerance));
    }
}
