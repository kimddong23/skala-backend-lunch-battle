package com.skala.lunch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 사원·식당·리뷰 CRUD 와 입력 검증. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("CRUD API")
class CrudApiTest {

    @Autowired MockMvc mockMvc;

    // ── 사원 ──

    @Test
    @DisplayName("초기 사원은 12명, 부서별 조회가 된다")
    void 사원_조회() throws Exception {
        mockMvc.perform(get("/api/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(12));
        mockMvc.perform(get("/api/members/department/개발팀"))
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(4)));
    }

    @Test
    @DisplayName("로그인 ID 중복은 409, 값을 그대로 수정하면 통과")
    void 사원_중복() throws Exception {
        mockMvc.perform(post("/api/members").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"kim","name":"중복","department":"개발팀"}
                                """))
                .andExpect(status().isConflict());

        // 자기 자신은 중복 검사에서 빠져야 한다
        mockMvc.perform(put("/api/members/1").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"kim","name":"김개발","department":"개발팀"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("투표 기록이 있는 사원은 삭제할 수 없다")
    void 사원_삭제_거부() throws Exception {
        mockMvc.perform(delete("/api/members/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("삭제할 수 없습니다")));
    }

    // ── 식당 ──

    @Test
    @DisplayName("전체 16곳 · 영업중 15곳 · 평점과 우승 횟수가 함께 나온다")
    void 식당_조회() throws Exception {
        // 개수를 박아 두면 식당을 하나 더 넣을 때마다 깨진다.
        // 전체가 영업중보다 많다는 관계만 본다 (폐업이 섞여 있으므로).
        String all = mockMvc.perform(get("/api/restaurants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThan(0)))
                .andReturn().getResponse().getContentAsString();
        String open = mockMvc.perform(get("/api/restaurants/active"))
                .andReturn().getResponse().getContentAsString();
        assertThat(com.jayway.jsonpath.JsonPath.<java.util.List<?>>read(open, "$[*].id").size())
                .as("영업중은 전체보다 적어야 한다 (폐업이 섞여 있다)")
                .isLessThan(com.jayway.jsonpath.JsonPath.<java.util.List<?>>read(all, "$[*].id").size());
        // 영업 중인 곳만 나온다 (초기 데이터에 폐업 1곳이 섞여 있다)
        mockMvc.perform(get("/api/restaurants/active"))
                .andExpect(jsonPath("$[*].active", everyItem(is(true))));
        // 평점은 리뷰 2건의 평균이어야 한다
        mockMvc.perform(get("/api/restaurants/1"))
                .andExpect(jsonPath("$.name").isNotEmpty())
                .andExpect(jsonPath("$.avgScore").value(4.5))
                .andExpect(jsonPath("$.reviewCount").value(2));
    }

    @Test
    @DisplayName("분류별·도보 시간별 조회")
    void 식당_필터() throws Exception {
        mockMvc.perform(get("/api/restaurants/category/한식"))
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(4)));
        mockMvc.perform(get("/api/restaurants/within/3"))
                .andExpect(jsonPath("$[*].walkMinutes", everyItem(lessThanOrEqualTo(3))));
    }

    @Test
    @DisplayName("기록이 있는 식당은 삭제 대신 영업 종료로 처리한다")
    void 식당_삭제_거부() throws Exception {
        mockMvc.perform(delete("/api/restaurants/2"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("영업 종료")));
    }

    @Test
    @DisplayName("가격·도보 시간 상한과 하한을 지킨다")
    void 식당_검증() throws Exception {
        mockMvc.perform(post("/api/restaurants").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","category":"한식","walkMinutes":-1,"price":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.walkMinutes").exists())
                .andExpect(jsonPath("$.errors.price").exists());

        mockMvc.perform(post("/api/restaurants").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"너무먼집","category":"한식","walkMinutes":999,"price":9999999999}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.walkMinutes").exists())
                .andExpect(jsonPath("$.errors.price").exists());
    }

    @Test
    @DisplayName("정의되지 않은 분류는 500 이 아니라 400")
    void 없는_분류() throws Exception {
        mockMvc.perform(post("/api/restaurants").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"우주식당","category":"우주식","walkMinutes":5,"price":9000}
                                """))
                .andExpect(status().isBadRequest());
    }

    // ── 리뷰 ──

    @Test
    @DisplayName("같은 사람이 같은 식당에 다시 쓰면 새로 쌓이지 않고 고쳐진다")
    void 리뷰_수정() throws Exception {
        mockMvc.perform(get("/api/reviews/restaurant/1"))
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(post("/api/reviews").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"memberId":1,"restaurantId":1,"score":1,"comment":"오늘은 별로"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(1));

        // 건수는 그대로, 평점만 바뀐다
        mockMvc.perform(get("/api/reviews/restaurant/1"))
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(get("/api/restaurants/1"))
                .andExpect(jsonPath("$.avgScore").value(2.5));
    }

    @Test
    @DisplayName("점수는 1~5점만")
    void 리뷰_점수_범위() throws Exception {
        for (String score : new String[]{"0", "6"}) {
            mockMvc.perform(post("/api/reviews").contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"memberId":1,"restaurantId":1,"score":%s}
                                    """.formatted(score)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.score").exists());
        }
    }

    // ── 공통 ──

    @Test
    @DisplayName("없는 자원은 404, 잘못된 타입은 400, 없는 주소는 404")
    void 오류_매핑() throws Exception {
        mockMvc.perform(get("/api/members/9999")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/restaurants/9999")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/battles/9999")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/restaurants/abc")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/없는주소")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Actuator 가 열려 있다")
    void actuator() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.db.status").value("UP"));
        // 표시 문구를 박아 두면 이름을 다듬을 때마다 테스트가 깨진다.
        // info 가 앱 정보를 실어 나르는지, 버전이 빌드와 맞는지를 본다.
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app.name").isNotEmpty())
                .andExpect(jsonPath("$.app.description").isNotEmpty())
                .andExpect(jsonPath("$.app.version").value("1.0.0"));
    }
}
