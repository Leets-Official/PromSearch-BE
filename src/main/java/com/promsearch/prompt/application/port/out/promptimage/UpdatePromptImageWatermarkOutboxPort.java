package com.promsearch.prompt.application.port.out.promptimage;

import java.time.Instant;
import java.util.UUID;

/** Outbox 발행 성공 또는 실패 결과를 저장 */
public interface UpdatePromptImageWatermarkOutboxPort {

    /** 외부 대기열 발행 성공 시각 저장 */
    void markPublished(UUID eventId, Instant publishedAt);

    /** 발행 실패 원인과 다음 발행 가능 시각 저장 */
    void reschedule(UUID eventId, Instant availableAt, String lastError);
}
