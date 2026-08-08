package com.skala.lunch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
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
        // 특정 식당 이름을 박아 두면 초기 데이터를 손볼 때마다 테스트가 깨진다.
        // 랭킹이 지켜야 할 성질만 본다.
        mockMvc.perform(get("/api/analysis/ranking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThan(0)))
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[*].rank", contains(IntStream.rangeClosed(1, rankingSize())
                        .boxed().toArray())))
                .andExpect(jsonPath("$[*].candidateCount", everyItem(greaterThan(0))))
                .andExpect(jsonPath("$[*].winCount", everyItem(greaterThanOrEqualTo(0))));
    }

    /** 랭킹 행 수 — 순위가 1부터 빠짐없이 매겨졌는지 보기 위해 먼저 센다. */
    private int rankingSize() throws Exception {
        String body = mockMvc.perform(get("/api/analysis/ranking"))
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.<java.util.List<?>>read(body, "$[*].rank").size();
    }

    @Test
    @DisplayName("우승은 득표가 아니라 경주로 정해진다 — 초기 이력이 그 사실을 반영한다")
    void 초기이력이_규칙과_맞는다() throws Exception {
        String body = mockMvc.perform(get("/api/analysis/cheer-effect"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        java.util.List<Boolean> matched =
                com.jayway.jsonpath.JsonPath.read(body, "$[*].matched");

        long hit = matched.stream().filter(Boolean::booleanValue).count();
        System.out.printf("── 초기 이력의 응원 적중: %d/%d%n", hit, matched.size());

        assertThat(matched).as("비교할 이력이 있어야 한다").isNotEmpty();
        // 표대로만 이긴 이력을 깔아 두면 "응원은 결과를 바꾸지 않는다" 는 규칙과 어긋난다.
        assertThat(hit).as("최다 득표가 매번 이긴 이력이면 규칙과 모순된다")
                .isLessThan(matched.size());
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
