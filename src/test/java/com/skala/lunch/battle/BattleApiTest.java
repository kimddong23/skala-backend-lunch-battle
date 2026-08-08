package com.skala.lunch.battle;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 배틀 진행 — 후보 등록 · 투표 · 마감.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("배틀 API")
class BattleApiTest {

    @Autowired MockMvc mockMvc;

    private Long battleId;
    private final String today = LocalDate.now().toString();
    private final String closesAt = LocalDate.now() + "T23:59:00";

    @BeforeEach
    void 오늘_배틀을_연다() throws Exception {
        String body = mockMvc.perform(post("/api/battles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"battleDate":"%s","closesAt":"%s"}
                                """.formatted(today, closesAt)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        battleId = JsonPath.parse(body).read("$.id", Long.class);
    }

    private long addCandidate(long restaurantId, long memberId) throws Exception {
        String body = mockMvc.perform(post("/api/battles/" + battleId + "/candidates")
                        .param("restaurantId", String.valueOf(restaurantId))
                        .param("memberId", String.valueOf(memberId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.parse(body).read("$.id", Long.class);
    }

    private void vote(long memberId, long candidateId) throws Exception {
        mockMvc.perform(post("/api/battles/" + battleId + "/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"memberId":%d,"candidateId":%d}
                                """.formatted(memberId, candidateId)))
                .andExpect(status().isOk());
    }

    // ── 배틀 ──

    @Test
    @DisplayName("같은 날짜로 두 번 열 수 없다")
    void 날짜_중복() throws Exception {
        mockMvc.perform(post("/api/battles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"battleDate":"%s","closesAt":"%s"}
                                """.formatted(today, closesAt)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("마감 시각이 이미 지났으면 만들 수 없다")
    void 지난_마감() throws Exception {
        // 마감이 과거면 한 표도 받을 수 없는 배틀이 만들어진다.
        // data.sql 의 지난 배틀(1~5일 전)과 겹치지 않는 날짜를 쓴다 —
        // 날짜 중복 검사가 먼저 걸리면 이 규칙을 확인할 수 없다.
        LocalDate past = LocalDate.now().minusDays(10);
        mockMvc.perform(post("/api/battles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"battleDate":"%s","closesAt":"%sT12:00:00"}
                                """.formatted(past, past)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("이미 지났습니다")));
    }

    @Test
    @DisplayName("마감 시각은 배틀 날짜와 같은 날이어야 한다")
    void 다른_날_마감() throws Exception {
        mockMvc.perform(post("/api/battles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"battleDate":"%s","closesAt":"%sT12:00:00"}
                                """.formatted(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2))))
                .andExpect(status().isBadRequest());
    }

    // ── 후보 ──

    @Test
    @DisplayName("같은 식당을 두 번 올릴 수 없다 — 표가 갈리기 때문")
    void 후보_중복() throws Exception {
        addCandidate(1, 1);
        mockMvc.perform(post("/api/battles/" + battleId + "/candidates")
                        .param("restaurantId", "1").param("memberId", "2"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("영업하지 않는 식당은 후보로 올릴 수 없다")
    void 폐업_식당() throws Exception {
        // 번호를 박아 두면 초기 데이터가 바뀔 때 조용히 다른 식당을 찌르게 된다.
        // 전체 목록에서 영업하지 않는 곳을 직접 찾아 쓴다.
        String all = mockMvc.perform(get("/api/restaurants"))
                .andReturn().getResponse().getContentAsString();
        java.util.List<Integer> closed = com.jayway.jsonpath.JsonPath
                .read(all, "$[?(@.active == false)].id");
        org.assertj.core.api.Assertions.assertThat(closed)
                .as("초기 데이터에 폐업 식당이 있어야 이 검사가 의미를 가진다").isNotEmpty();

        mockMvc.perform(post("/api/battles/" + battleId + "/candidates")
                        .param("restaurantId", String.valueOf(closed.get(0)))
                        .param("memberId", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("없는 식당·사원은 404")
    void 없는_대상() throws Exception {
        mockMvc.perform(post("/api/battles/" + battleId + "/candidates")
                        .param("restaurantId", "9999").param("memberId", "1"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/battles/" + battleId + "/candidates")
                        .param("restaurantId", "1").param("memberId", "9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("표를 받은 후보는 내릴 수 없다")
    void 득표한_후보_삭제() throws Exception {
        long c = addCandidate(1, 1);
        vote(1, c);
        mockMvc.perform(delete("/api/battles/" + battleId + "/candidates/" + c))
                .andExpect(status().isConflict());
    }

    // ── 투표 ──

    @Test
    @DisplayName("투표하면 득표 수와 총 투표 수가 함께 오른다")
    void 투표_반영() throws Exception {
        long c = addCandidate(1, 1);
        vote(1, c);
        vote(2, c);

        mockMvc.perform(get("/api/battles/" + battleId))
                .andExpect(jsonPath("$.totalVotes").value(2))
                .andExpect(jsonPath("$.candidates[0].voteCount").value(2))
                .andExpect(jsonPath("$.candidates[0].sharePercent").value(100.0));
    }

    @Test
    @DisplayName("한 사람은 한 배틀에 한 표만 — 다른 후보로도 못 찍는다")
    void 일인_일표() throws Exception {
        long a = addCandidate(1, 1);
        long b = addCandidate(3, 1);
        vote(1, a);

        mockMvc.perform(post("/api/battles/" + battleId + "/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"memberId":1,"candidateId":%d}
                                """.formatted(b)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("이미 투표")));
    }

    @Test
    @DisplayName("투표를 취소하면 득표 수가 되돌아간다")
    void 투표_취소() throws Exception {
        long c = addCandidate(1, 1);
        vote(1, c);

        mockMvc.perform(delete("/api/battles/" + battleId + "/votes").param("memberId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVotes").value(0))
                .andExpect(jsonPath("$.candidates[0].voteCount").value(0));

        // 취소했으면 다시 찍을 수 있어야 한다
        vote(1, c);
    }

    @Test
    @DisplayName("다른 배틀의 후보에는 투표할 수 없다")
    void 남의_배틀_후보() throws Exception {
        long c = addCandidate(1, 1);
        String other = mockMvc.perform(post("/api/battles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"battleDate":"%s","closesAt":"%sT23:59:00"}
                                """.formatted(LocalDate.now().plusDays(1), LocalDate.now().plusDays(1))))
                .andReturn().getResponse().getContentAsString();
        long otherId = JsonPath.parse(other).read("$.id", Long.class);

        mockMvc.perform(post("/api/battles/" + otherId + "/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"memberId":1,"candidateId":%d}
                                """.formatted(c)))
                .andExpect(status().isBadRequest());
    }

    // ── 마감 ──

    @Test
    @DisplayName("표만으로 마감하면 최다 득표가 이긴다")
    void 득표로_마감() throws Exception {
        // 식당 이름을 박아 두면 초기 데이터를 손볼 때마다 깨진다. 조회해서 쓴다.
        String more = restaurantName(2), less = restaurantName(1);

        long a = addCandidate(2, 1);
        long b = addCandidate(1, 1);

        for (long m : new long[]{1, 2, 3, 4, 5}) vote(m, a);   // 5표
        for (long m : new long[]{6, 7, 8, 9}) vote(m, b);      // 4표

        mockMvc.perform(get("/api/battles/" + battleId))
                .andExpect(jsonPath("$.candidates[0].restaurantName").value(more))
                .andExpect(jsonPath("$.candidates[0].voteCount").value(5))
                .andExpect(jsonPath("$.candidates[1].restaurantName").value(less))
                .andExpect(jsonPath("$.candidates[1].voteCount").value(4));

        mockMvc.perform(post("/api/battles/" + battleId + "/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.winnerName").value(more));
    }

    /** 초기 데이터의 식당 이름을 조회해 온다. */
    private String restaurantName(long id) throws Exception {
        String body = mockMvc.perform(get("/api/restaurants/" + id))
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(body, "$.name");
    }



    @Test
    @DisplayName("후보가 없으면 마감할 수 없다")
    void 후보_없이_마감() throws Exception {
        mockMvc.perform(post("/api/battles/" + battleId + "/close"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("마감 후에는 투표·후보 등록·재마감이 모두 막힌다")
    void 마감_후_잠금() throws Exception {
        long c = addCandidate(1, 1);
        vote(1, c);
        mockMvc.perform(post("/api/battles/" + battleId + "/close")).andExpect(status().isOk());

        mockMvc.perform(post("/api/battles/" + battleId + "/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"memberId":2,"candidateId":%d}
                                """.formatted(c)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/battles/" + battleId + "/candidates")
                        .param("restaurantId", "3").param("memberId", "1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/battles/" + battleId + "/close"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("후보·투표가 있는 배틀은 삭제할 수 없다")
    void 배틀_삭제_거부() throws Exception {
        addCandidate(1, 1);
        mockMvc.perform(delete("/api/battles/" + battleId))
                .andExpect(status().isConflict());
    }

    // ── 검증 ──

    @Test
    @DisplayName("필수값 누락·잘못된 본문은 400")
    void 입력_검증() throws Exception {
        mockMvc.perform(post("/api/battles/" + battleId + "/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.memberId").exists())
                .andExpect(jsonPath("$.errors.candidateId").exists());

        mockMvc.perform(post("/api/battles/" + battleId + "/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":"))
                .andExpect(status().isBadRequest());
    }
}
