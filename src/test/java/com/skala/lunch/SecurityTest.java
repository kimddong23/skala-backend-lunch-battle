package com.skala.lunch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 보안 회귀 방지.
 *
 * 한 번 막은 구멍이 조용히 다시 열리는 것을 막는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("보안")
class SecurityTest {

    @Autowired MockMvc mockMvc;

    @Test
    @DisplayName("화면이 사용자 입력을 이스케이프한다 — 안 하면 식당명으로 스크립트를 심을 수 있다")
    void 화면_이스케이프() throws Exception {
        String html = new String(new ClassPathResource("static/index.html")
                .getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertThat(html).as("이스케이프 함수가 있어야 함").contains("const esc =");

        // innerHTML 에 들어가는 부분만 본다.
        // textContent 로 넣는 곳은 브라우저가 태그로 해석하지 않으므로 안전하다.
        Pattern block = Pattern.compile("innerHTML\\s*=\\s*(.*?);\\n", Pattern.DOTALL);
        Pattern unescaped = Pattern.compile(
                "\\$\\{(?!esc\\()[A-Za-z_][A-Za-z0-9_]*\\."
                        + "(restaurantName|name|category|department|addedByName"
                        + "|scouting|penaltyNote|verdict|memberName|winnerName|comment)\\b");

        StringBuilder found = new StringBuilder();
        Matcher b = block.matcher(html);
        while (b.find()) {
            Matcher u = unescaped.matcher(b.group(1));
            while (u.find()) {
                found.append(u.group()).append(' ');
            }
        }
        assertThat(found.toString()).as("innerHTML 안에서 이스케이프를 거치지 않은 출력").isEmpty();

        // 이스케이프 자리가 실제로 쓰이는지도 확인 — 함수만 있고 안 쓰면 의미가 없다
        assertThat(html.split("esc\\(", -1).length - 1)
                .as("esc() 호출 횟수").isGreaterThanOrEqualTo(8);
    }

    @Test
    @DisplayName("스크립트를 담은 식당명은 저장돼도 JSON 문자열로만 나간다")
    void 저장은_되되_실행되지_않는다() throws Exception {
        String payload = "<img src=x onerror=alert(1)>떡볶이";
        mockMvc.perform(post("/api/restaurants").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","category":"분식","walkMinutes":3,"price":8000}
                                """.formatted(payload)))
                .andExpect(status().isCreated())
                // JSON 응답에서는 그대로 담기는 게 정상이다. 위험은 화면이 태그로 해석할 때 생긴다.
                .andExpect(jsonPath("$.name").value(payload));
    }

    @Test
    @DisplayName("SQL 주입 문자열은 조건값으로만 쓰인다")
    void sql_주입() throws Exception {
        mockMvc.perform(get("/api/members/department/{d}", "' OR '1'='1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));   // 그런 부서는 없다
    }

    @Test
    @DisplayName("H2 콘솔과 내부 구조를 드러내는 Actuator 경로는 닫혀 있다")
    void 관리_경로_차단() throws Exception {
        for (String path : new String[]{"/h2-console", "/actuator/env",
                                        "/actuator/beans", "/actuator/mappings"}) {
            mockMvc.perform(get(path))
                    .andExpect(status().isNotFound());
        }
        // 상태 확인용은 열려 있어야 한다
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/info")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("오류 응답에 스택 트레이스나 내부 경로가 섞이지 않는다")
    void 오류_노출() throws Exception {
        String body = mockMvc.perform(get("/api/members/9999"))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("Exception").doesNotContain("at com.skala")
                .doesNotContain("/Users").doesNotContain("jdbc:");
    }
}
