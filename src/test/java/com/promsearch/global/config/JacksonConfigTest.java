package com.promsearch.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.context.annotation.Import;

@JsonTest(properties = "spring.jackson.time-zone=Asia/Seoul")
@Import(JacksonConfig.class)
class JacksonConfigTest {

    @Autowired
    private ObjectMapper objectMapper;

    @DisplayName("Instant 응답은 동일한 시점을 한국 시간 오프셋으로 직렬화한다")
    @Test
    void serializeInstantWithKoreanOffset() throws Exception {
        TimeResponse response = new TimeResponse(Instant.parse("2026-07-28T12:00:00Z"));

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).isEqualTo("""
                {"occurredAt":"2026-07-28T21:00:00+09:00"}\
                """);
    }

    @DisplayName("숫자 응답은 JSON 숫자 타입으로 직렬화한다")
    @Test
    void serializeNumbersAsJsonNumbers() throws Exception {
        NumericResponse response = new NumericResponse(1L, 0, 2, 3600L, 128L);

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertThat(json.path("tagId").isIntegralNumber()).isTrue();
        assertThat(json.path("page").isIntegralNumber()).isTrue();
        assertThat(json.path("size").isIntegralNumber()).isTrue();
        assertThat(json.path("totalElements").isIntegralNumber()).isTrue();
        assertThat(json.path("expiresIn").isIntegralNumber()).isTrue();
    }

    private record TimeResponse(Instant occurredAt) {
    }

    private record NumericResponse(long tagId, int page, int size, long totalElements, long expiresIn) {
    }
}
