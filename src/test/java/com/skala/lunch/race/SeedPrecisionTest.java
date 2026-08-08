package com.skala.lunch.race;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 시드가 브라우저까지 손상 없이 도달하는지 확인한다.
 *
 * 시드는 "같은 경기를 다시 볼 수 있다"는 약속의 근거다. 값이 한 자리라도 바뀌면
 * 그 약속이 깨지는데, 숫자로 내보내면 서버도 테스트도 멀쩡한 채로 브라우저에서만
 * 조용히 끝자리가 뭉개진다. 그래서 전송 형식 자체를 고정해 둔다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("시드 정밀도")
class SeedPrecisionTest {

    /** 자바스크립트 number 가 정확히 표현할 수 있는 최대 정수. */
    private static final long JS_MAX_SAFE_INTEGER = 9_007_199_254_740_991L;

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("시드를 문자열로 내보낸다 — 숫자로 보내면 브라우저가 끝자리를 반올림한다")
    void 시드_문자열_전송() throws Exception {
        LocalDate date = LocalDate.now().plusDays(4100);
        String battleBody = mockMvc.perform(post("/api/battles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"battleDate":"%s","closesAt":"%sT23:59:00"}
                                """.formatted(date, date)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long battleId = objectMapper.readTree(battleBody).get("id").asLong();

        for (long restaurantId : new long[]{1, 3}) {
            mockMvc.perform(post("/api/battles/{id}/candidates", battleId)
                            .param("restaurantId", String.valueOf(restaurantId))
                            .param("memberId", "1"))
                    .andExpect(status().isCreated());
        }

        String raceBody = mockMvc.perform(post("/api/battles/{id}/race", battleId))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode seed = objectMapper.readTree(raceBody).get("seed");

        assertThat(seed.isTextual())
                .as("시드는 문자열이어야 한다. 숫자면 브라우저에서 정밀도가 깨진다")
                .isTrue();
        assertThat(seed.asText()).matches("-?\\d+");

        // 실제로 위험한 크기인지 확인한다. 안전 범위 안이면 이 방어는 의미가 없다.
        long value = Long.parseLong(seed.asText());
        assertThat(Math.abs(value))
                .as("시드는 자바스크립트 안전 정수 범위를 넘는 크기다")
                .isGreaterThan(JS_MAX_SAFE_INTEGER);
    }

    @Test
    @DisplayName("화면이 시드를 문자열 그대로 읽는다 — Number 로 바꾸면 정밀도가 깨진다")
    void 화면이_시드를_숫자로_바꾸지_않는다() throws Exception {
        String html = new String(new org.springframework.core.io.ClassPathResource("static/index.html")
                .getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);

        assertThat(html)
                .as("시드를 Number() 로 감싸면 문자열 전송이 무의미해진다")
                .doesNotContain("Number(raceData.seed)")
                .doesNotContain("parseInt(raceData.seed");
    }
}
