package com.promsearch.prompt.application.usecase.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PromptImageWatermarkJobTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @DisplayName("워터마크 작업 메시지를 JSON으로 직렬화하고 동일한 값으로 복원한다")
    @Test
    void serializeAndDeserialize() throws Exception {
        PromptImageWatermarkJob job = createJob();

        String json = objectMapper.writeValueAsString(job);
        PromptImageWatermarkJob restored =
                objectMapper.readValue(json, PromptImageWatermarkJob.class);

        assertThat(restored).isEqualTo(job);
    }

    @DisplayName("원본과 결과 Object Key가 같으면 메시지를 생성할 수 없다")
    @Test
    void rejectSameObjectKey() {
        UUID eventId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();
        String objectKey = "prompt-images/original/1/image.png";

        assertThatThrownBy(() -> new PromptImageWatermarkJob(
                PromptImageWatermarkJob.CURRENT_EVENT_VERSION,
                eventId,
                imageId,
                objectKey,
                objectKey,
                "image/png",
                1,
                Instant.parse("2026-07-27T00:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("원본과 워터마크 결과 Object Key는 달라야 합니다.");
    }

    private PromptImageWatermarkJob createJob() {
        return new PromptImageWatermarkJob(
                PromptImageWatermarkJob.CURRENT_EVENT_VERSION,
                UUID.fromString("08f4bba0-40a7-4bb4-a847-8dd3645bd9c7"),
                UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                "prompt-images/original/10/123e4567-e89b-12d3-a456-426614174000.png",
                "prompt-images/watermarked/10/123e4567-e89b-12d3-a456-426614174000.png",
                "IMAGE/PNG",
                1,
                Instant.parse("2026-07-27T00:00:00Z")
        );
    }
}
