package com.promsearch.prompt.infrastructure.persistence.entity;

import com.promsearch.common.BaseEntity;
import com.promsearch.prompt.application.usecase.dto.PromptImageWatermarkJob;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DB 커밋과 SQS 전송 사이의 유실을 막는 워터마크 작업 Outbox
 *
 * <p>API 트랜잭션은 PENDING 레코드까지만 저장하고, 별도 발행기가 SQS 전송 성공 후
 * PUBLISHED로 변경한다.</p>
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "prompt_image_watermark_outbox",
        indexes = {
                @Index(
                        name = "idx_prompt_image_watermark_outbox_publish",
                        columnList = "status,available_at,created_at"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_prompt_image_watermark_outbox_image_version",
                        columnNames = {"image_id", "processing_version"}
                )
        }
)
public class PromptImageWatermarkOutboxJpaEntity extends BaseEntity {

    public enum Status {
        PENDING,
        PUBLISHED
    }

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "image_id", nullable = false, updatable = false)
    private UUID imageId;

    @Column(name = "event_type", nullable = false, updatable = false, length = 100)
    private String eventType;

    @Column(name = "event_version", nullable = false, updatable = false)
    private Integer eventVersion;

    @Column(name = "processing_version", nullable = false, updatable = false)
    private Integer processingVersion;

    @Column(name = "payload", nullable = false, updatable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "last_error", length = 1_000)
    private String lastError;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion;

    private PromptImageWatermarkOutboxJpaEntity(
            UUID eventId,
            UUID imageId,
            String eventType,
            Integer eventVersion,
            Integer processingVersion,
            String payload,
            Status status,
            Integer attemptCount,
            Instant availableAt
    ) {
        this.eventId = eventId;
        this.imageId = imageId;
        this.eventType = eventType;
        this.eventVersion = eventVersion;
        this.processingVersion = processingVersion;
        this.payload = payload;
        this.status = status;
        this.attemptCount = attemptCount;
        this.availableAt = availableAt;
    }

    /** 새 작업을 즉시 발행 가능한 PENDING 상태로 생성 */
    public static PromptImageWatermarkOutboxJpaEntity pending(
            PromptImageWatermarkJob job,
            String payload
    ) {
        return new PromptImageWatermarkOutboxJpaEntity(
                job.eventId(),
                job.imageId(),
                PromptImageWatermarkJob.EVENT_TYPE,
                job.eventVersion(),
                job.processingVersion(),
                payload,
                Status.PENDING,
                0,
                job.occurredAt()
        );
    }

    /** 발행기가 외부 호출을 수행하는 동안 다른 인스턴스가 선택하지 않도록 임대 */
    public void claimUntil(Instant claimedUntil) {
        requirePending();
        if (claimedUntil == null) {
            throw new IllegalArgumentException("Outbox 선점 만료 시각은 필수입니다.");
        }
        availableAt = claimedUntil;
    }

    /** 메시지 대기열 전송 성공을 멱등하게 반영 */
    public void markPublished(Instant publishedAt) {
        if (status == Status.PUBLISHED) {
            return;
        }
        if (publishedAt == null) {
            throw new IllegalArgumentException("Outbox 발행 시각은 필수입니다.");
        }
        status = Status.PUBLISHED;
        this.publishedAt = publishedAt;
        lastError = null;
    }

    /** 전송 실패 횟수와 오류를 저장하고 다음 발행 가능 시각으로 이동 */
    public void reschedule(Instant nextAvailableAt, String error) {
        requirePending();
        if (nextAvailableAt == null || error == null || error.isBlank()) {
            throw new IllegalArgumentException("Outbox 재시도 정보가 유효하지 않습니다.");
        }
        attemptCount++;
        availableAt = nextAvailableAt;
        lastError = error;
    }

    /** 발행 완료 작업이 다시 선점되거나 재예약되는 잘못된 상태 전이를 차단 */
    private void requirePending() {
        if (status != Status.PENDING) {
            throw new IllegalStateException("PENDING 상태의 Outbox 작업만 변경할 수 있습니다.");
        }
    }
}
