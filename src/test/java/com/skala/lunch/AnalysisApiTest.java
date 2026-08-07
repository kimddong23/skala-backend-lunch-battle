package com.skala.lunch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 분석 6종 — SQL Mapper 로 처리하는 집계. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("분석 API (MyBatis)")
class AnalysisApiTest {

    @Autowired MockMvc mockMvc;

    @Test
    @DisplayName("식당 랭킹 — 후보로 오른 적 있는 식당만, 우승 많은 순")
    void 랭킹() throws Exception {
        mockMvc.perform(get("/api/analysis/ranking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[0].restaurantName").value("김치찌개의민족"))
                .andExpect(jsonPath("$[0].winCount").value(3))
                .andExpect(jsonPath("$[*].candidateCount", everyItem(greaterThan(0))));
    }

    @Test
    @DisplayName("부서별 취향 — 부서 안에서의 비중 합이 100%")
    void 부서별_취향() throws Exception {
        mockMvc.perform(get("/api/analysis/department-taste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThan(0)))
                .andExpect(jsonPath("$[*].sharePercent", everyItem(greaterThan(0.0))));
    }

    @Test
    @DisplayName("편식 지수 — 비중과 판정이 함께 나온다")
    void 편식_지수() throws Exception {
        mockMvc.perform(get("/api/analysis/picky-index"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pickyPercent").value(greaterThan(0.0)))
                .andExpect(jsonPath("$[0].verdict").isNotEmpty())
                .andExpect(jsonPath("$[*].topCategoryVotes",
                        everyItem(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("분류별 점유 — 점유율 합이 100%")
    void 분류별_점유() throws Exception {
        mockMvc.perform(get("/api/analysis/category-share"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThan(0)));
    }

    @Test
    @DisplayName("요일별 경향 · 참여율")
    void 나머지() throws Exception {
        mockMvc.perform(get("/api/analysis/weekday-trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].weekdayName").isNotEmpty());
        mockMvc.perform(get("/api/analysis/participation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].memberCount", everyItem(is(12))))
                .andExpect(jsonPath("$[*].participationPercent",
                        everyItem(lessThanOrEqualTo(100.0))));
    }
}
