package com.promsearch.prompt.infrastructure.persistence;

import com.promsearch.prompt.application.port.out.promptimage.ClaimPromptImageWatermarkJobsPort;
import com.promsearch.prompt.application.port.out.promptimage.UpdatePromptImageWatermarkOutboxPort;
import com.promsearch.prompt.application.usecase.dto.PendingPromptImageWatermarkMessage;
import com.promsearch.prompt.infrastructure.persistence.entity.PromptImageWatermarkOutboxJpaEntity;
import com.promsearch.prompt.infrastructure.persistence.entity.PromptImageWatermarkOutboxJpaEntity.Status;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Outbox 작업 선점과 발행 결과 저장을 각각 짧은 DB 트랜잭션으로 처리 */
@Component
@RequiredArgsConstructor
public class PromptImageWatermarkOutboxPublicationAdapter
        implements ClaimPromptImageWatermarkJobsPort, UpdatePromptImageWatermarkOutboxPort {

    private final PromptImageWatermarkOutboxRepository outboxRepository;

    /** 비관적 잠금으로 작업을 선점하고 외부 전송에 필요한 메시지만 반환 */
    @Override
    @Transactional
    public List<PendingPromptImageWatermarkMessage> claimAvailable(
            Instant now,
            Instant claimedUntil,
            int batchSize
    ) {
        if (batchSize <= 0 || !claimedUntil.isAfter(now)) {
            throw new IllegalArgumentException("Outbox 선점 조건이 유효하지 않습니다.");
        }

        List<PromptImageWatermarkOutboxJpaEntity> claimed =
                outboxRepository.findPublishableForUpdate(
                        Status.PENDING,
                        now,
                        PageRequest.of(0, batchSize)
                );
        claimed.forEach(outbox -> outbox.claimUntil(claimedUntil));
        return claimed.stream()
                .map(outbox -> new PendingPromptImageWatermarkMessage(
                        outbox.getEventId(),
                        outbox.getPayload(),
                        outbox.getAttemptCount()
                ))
                .toList();
    }

    /** 발행 완료 Outbox를 PUBLISHED 상태로 변경 */
    @Override
    @Transactional
    public void markPublished(UUID eventId, Instant publishedAt) {
        getOutbox(eventId).markPublished(publishedAt);
    }

    /** 발행 실패 Outbox를 다음 재시도 시각으로 되돌림 */
    @Override
    @Transactional
    public void reschedule(UUID eventId, Instant availableAt, String lastError) {
        getOutbox(eventId).reschedule(availableAt, lastError);
    }

    /** 발행 결과를 반영할 Outbox를 조회하고 누락된 내부 상태는 즉시 차단 */
    private PromptImageWatermarkOutboxJpaEntity getOutbox(UUID eventId) {
        return outboxRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException(
                        "워터마크 Outbox 작업을 찾을 수 없습니다: " + eventId
                ));
    }
}
