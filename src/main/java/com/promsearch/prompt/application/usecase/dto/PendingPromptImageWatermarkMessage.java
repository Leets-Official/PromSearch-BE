package com.promsearch.prompt.application.usecase.dto;

import java.util.Objects;
import java.util.UUID;

/** Outbox에서 선점해 메시지 대기열로 보낼 워터마크 작업 */
public record PendingPromptImageWatermarkMessage(
        UUID eventId,
        String payload,
        int attemptCount
) {

    public PendingPromptImageWatermarkMessage {
        Objects.requireNonNull(eventId, "이벤트 식별자는 필수입니다.");
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("워터마크 작업 메시지는 필수입니다.");
        }
        if (attemptCount < 0) {
            throw new IllegalArgumentException("발행 시도 횟수는 0 이상이어야 합니다.");
        }
    }
}
