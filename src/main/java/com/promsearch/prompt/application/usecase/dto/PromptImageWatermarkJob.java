package com.promsearch.prompt.application.usecase.dto;

import com.promsearch.prompt.domain.enums.PromptImageContentType;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * API와 이미지 Worker 사이의 워터마크 작업 메시지
 *
 * <p>Object Key만 전달하며 S3 URL이나 이미지 바이너리는 메시지에 포함하지 않는다.</p>
 */
public record PromptImageWatermarkJob(
        int eventVersion,
        UUID eventId,
        UUID imageId,
        String originalObjectKey,
        String watermarkedObjectKey,
        String contentType,
        int processingVersion,
        Instant occurredAt
) {

    public static final int CURRENT_EVENT_VERSION = 1;
    public static final String EVENT_TYPE = "PROMPT_IMAGE_WATERMARK_REQUESTED";

    public PromptImageWatermarkJob {
        if (eventVersion != CURRENT_EVENT_VERSION) {
            throw new IllegalArgumentException("지원하지 않는 워터마크 작업 메시지 버전입니다.");
        }
        Objects.requireNonNull(eventId, "워터마크 작업 이벤트 식별자는 필수입니다.");
        Objects.requireNonNull(imageId, "이미지 식별자는 필수입니다.");
        Objects.requireNonNull(occurredAt, "워터마크 작업 생성 시각은 필수입니다.");
        if (processingVersion <= 0) {
            throw new IllegalArgumentException("워터마크 처리 버전은 0보다 커야 합니다.");
        }

        originalObjectKey = normalizeObjectKey(originalObjectKey, "원본 이미지 Object Key는 필수입니다.");
        watermarkedObjectKey = normalizeObjectKey(
                watermarkedObjectKey,
                "워터마크 결과 Object Key는 필수입니다."
        );
        if (originalObjectKey.equals(watermarkedObjectKey)) {
            throw new IllegalArgumentException("원본과 워터마크 결과 Object Key는 달라야 합니다.");
        }
        contentType = PromptImageContentType.fromMimeType(contentType).getMimeType();
    }

    /** 필수 Object Key의 공백을 정리하고 누락된 메시지를 생성 단계에서 차단 */
    private static String normalizeObjectKey(String objectKey, String message) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return objectKey.trim();
    }
}
