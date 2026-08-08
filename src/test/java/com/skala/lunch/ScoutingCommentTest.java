package com.skala.lunch;

import com.skala.lunch.race.RaceDto;
import com.skala.lunch.battle.BattleDto;
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
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 스카우팅 한줄평이 실제로 여러 갈래로 갈리는지 확인한다.
 *
 * 판정 기준이 스탯 범위 밖에 있으면 코드는 멀쩡히 컴파일되고 테스트도 통과하는데
 * 화면에는 모든 햄스터가 똑같은 말을 달고 나온다. 실제로 그런 상태였다 —
 * 속도 최저값이 3.0 인데 기준이 2.4 라 "발이 빠릅니다" 만 나왔다.
 */
@SpringBootTest
@Transactional
@DisplayName("스카우팅 한줄평")
class ScoutingCommentTest {

    @Autowired RaceService raceService;
    @Autowired BattleService battleService;

    @Test
    @DisplayName("판단력 평이 한 가지로 고정되지 않는다")
    void 판단력평이_갈린다() {
        Set<String> senseWords = new HashSet<>();

        for (int i = 0; i < 120; i++) {
            LocalDate date = LocalDate.now().plusDays(5000 + i);
            BattleDto b = battleService.openBattle(BattleDto.builder()
                    .battleDate(date)
                    .closesAt(LocalDateTime.of(date, LocalTime.of(23, 59)))
                    .build());
            for (long r : new long[]{1, 3, 4, 7, 9}) {
                battleService.addCandidate(b.getId(), r, 1L);
            }
            RaceDto race = raceService.run(b.getId());
            for (RaceDto.LaneDto lane : race.getLanes()) {
                senseWords.add(lane.getScouting().split(" · ")[0]);
            }
        }

        System.out.println("── 나온 판단력 평: " + senseWords);

        // 세 갈래가 모두 코드에 있으니 600마리쯤 뽑으면 전부 나와야 한다.
        assertThat(senseWords)
                .as("판단력 평 세 갈래가 모두 나와야 한다 (기준이 범위 밖이면 한 가지만 나온다)")
                .containsExactlyInAnyOrder("길눈이 밝습니다", "웬만하면 찾아갑니다", "막다른 길을 좋아합니다");
    }
}
