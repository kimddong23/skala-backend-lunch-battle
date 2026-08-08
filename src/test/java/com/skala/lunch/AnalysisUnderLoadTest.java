package com.skala.lunch;

import com.skala.lunch.dto.BattleDto;
import com.skala.lunch.dto.VoteRequestDto;
import com.skala.lunch.service.BattleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 표가 쌓인 뒤에도 분석 조회가 살아 있는지 확인한다.
 *
 * 초기 데이터만 놓고 한 번 호출해 보는 것으로는 부족했다. 요일별 조회는
 * 표가 적을 때는 멀쩡히 200 을 주다가, 어느 정도 쌓이면 그때부터 계속 500 이 났다.
 * H2 가 CASE 안의 식을 GROUP BY 항목과 같은 것으로 보지 않는 실행 계획을
 * 그 시점부터 고르기 때문이다 — 문법이 아니라 계획에 달린 문제라 작은 표본에서는
 * 절대 드러나지 않는다. 그래서 일부러 표를 쌓아 놓고 부른다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("분석 조회 (데이터가 쌓인 뒤)")
class AnalysisUnderLoadTest {

    private static final String[] ENDPOINTS = {
            "ranking", "department-taste", "picky-index",
            "category-share", "weekday-trend", "participation"
    };

    @Autowired MockMvc mockMvc;
    @Autowired BattleService battleService;

    /** 여러 요일·여러 분류에 걸쳐 표를 쌓는다. */
    private void buildHistory(int battles) {
        long memberCount = 12;
        long[] restaurants = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

        for (int i = 0; i < battles; i++) {
            LocalDate date = LocalDate.now().plusDays(200 + i);   // 요일이 골고루 섞이도록
            BattleDto b = battleService.openBattle(BattleDto.builder()
                    .battleDate(date)
                    .closesAt(LocalDateTime.of(date, LocalTime.of(23, 59)))
                    .build());

            List<Long> candIds = new ArrayList<>();
            for (int k = 0; k < 4; k++) {
                long r = restaurants[(i * 4 + k) % restaurants.length];
                candIds.add(battleService.addCandidate(b.getId(), r, 1L).getId());
            }
            for (long m = 1; m <= memberCount; m++) {
                battleService.vote(b.getId(), VoteRequestDto.builder()
                        .memberId(m)
                        .candidateId(candIds.get((int) (m % candIds.size())))
                        .build());
            }
        }
    }

    @Test
    @DisplayName("표가 많이 쌓여도 여섯 가지 분석이 모두 200을 준다")
    void 분석_전부_살아있다() throws Exception {
        // 쌓기 전에도 되는지 먼저 본다 (되던 것이 깨지는 지점을 구분하기 위해)
        for (String key : ENDPOINTS) {
            mockMvc.perform(get("/api/analysis/" + key)).andExpect(status().isOk());
        }

        buildHistory(14);       // 배틀 14건 x 12표 = 168표

        for (String key : ENDPOINTS) {
            mockMvc.perform(get("/api/analysis/" + key))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Test
    @DisplayName("요일별 조회가 요일 이름을 제대로 붙여 준다")
    void 요일_이름이_붙는다() throws Exception {
        buildHistory(10);

        mockMvc.perform(get("/api/analysis/weekday-trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].weekdayName").isNotEmpty())
                .andExpect(jsonPath("$[0].weekday").isNumber())
                .andExpect(jsonPath("$[0].category").isNotEmpty())
                .andExpect(jsonPath("$[0].voteCount").isNumber());
    }
}
