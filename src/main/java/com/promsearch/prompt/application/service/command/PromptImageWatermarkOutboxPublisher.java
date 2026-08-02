package com.promsearch.prompt.application.service.command;

import com.promsearch.prompt.application.port.out.promptimage.ClaimPromptImageWatermarkJobsPort;
import com.promsearch.prompt.application.port.out.promptimage.PublishPromptImageWatermarkJobPort;
import com.promsearch.prompt.application.port.out.promptimage.UpdatePromptImageWatermarkOutboxPort;
import com.promsearch.prompt.application.usecase.PublishPendingPromptImageWatermarkJobsUseCase;
import com.promsearch.prompt.application.usecase.dto.PendingPromptImageWatermarkMessage;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** DB 선점과 상태 변경은 짧은 트랜잭션으로 위임하고 외부 메시지 발행은 트랜잭션 없이 수행 */
public class PromptImageWatermarkOutboxPublisher
        implements PublishPendingPromptImageWatermarkJobsUseCase {

    private static final int MAX_ERROR_LENGTH = 1_000;

    private final ClaimPromptImageWatermarkJobsPort claimJobsPort;
    private final PublishPromptImageWatermarkJobPort publishJobPort;
    private final UpdatePromptImageWatermarkOutboxPort updateOutboxPort;
    private final PublicationPolicy policy;
    private final Clock clock;

    public PromptImageWatermarkOutboxPublisher(
            ClaimPromptImageWatermarkJobsPort claimJobsPort,
            PublishPromptImageWatermarkJobPort publishJobPort,
            UpdatePromptImageWatermarkOutboxPort updateOutboxPort,
            PublicationPolicy policy,
            Clock clock
    ) {
        this.claimJobsPort = Objects.requireNonNull(claimJobsPort);
        this.publishJobPort = Objects.requireNonNull(publishJobPort);
        this.updateOutboxPort = Objects.requireNonNull(updateOutboxPort);
        this.policy = Objects.requireNonNull(policy);
        this.clock = Objects.requireNonNull(clock);
    }

    /** 한 배치를 선점한 뒤 각 작업을 발행하고 성공·실패 상태를 개별 반영 */
    @Override
    public PublicationResult publishAvailable() {
        Instant claimedAt = Instant.now(clock);
        List<PendingPromptImageWatermarkMessage> messages = claimJobsPort.claimAvailable(
                claimedAt,
                claimedAt.plus(policy.claimLease()),
                policy.batchSize()
        );

        int publishedCount = 0;
        int failedCount = 0;
        // TODO: 발행 지연이 병목일 때만 제한된 전송 풀·SendMessageBatch를 도입하고,
        // eventId별 성공 여부를 개별 반영해 부분 실패의 재시도 의미를 유지
        for (PendingPromptImageWatermarkMessage message : messages) {
            try {
                publishJobPort.publish(message);
                updateOutboxPort.markPublished(message.eventId(), Instant.now(clock));
                publishedCount++;
            } catch (RuntimeException exception) {
                updateOutboxPort.reschedule(
                        message.eventId(),
                        Instant.now(clock).plus(retryDelay(message.attemptCount() + 1)),
                        errorMessage(exception)
                );
                failedCount++;
            }
        }

        return new PublicationResult(messages.size(), publishedCount, failedCount);
    }

    /** 재시도 횟수에 따라 증가하되 설정된 최댓값을 넘지 않는 지수 백오프 계산 */
    private Duration retryDelay(int nextAttemptCount) {
        Duration delay = policy.initialRetryDelay();
        for (int attempt = 1;
             attempt < nextAttemptCount && delay.compareTo(policy.maximumRetryDelay()) < 0;
             attempt++) {
            if (delay.compareTo(policy.maximumRetryDelay().dividedBy(2)) > 0) {
                return policy.maximumRetryDelay();
            }
            delay = delay.multipliedBy(2);
        }
        return delay.compareTo(policy.maximumRetryDelay()) > 0
                ? policy.maximumRetryDelay()
                : delay;
    }

    /** DB 컬럼 제한을 지키면서 운영자가 원인을 식별할 수 있는 오류 문구 생성 */
    private String errorMessage(RuntimeException exception) {
        String message = exception.getMessage();
        String resolved = message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;
        return resolved.length() <= MAX_ERROR_LENGTH
                ? resolved
                : resolved.substring(0, MAX_ERROR_LENGTH);
    }

    /** Outbox 배치 크기, 선점 임대와 지수 백오프 정책 */
    public record PublicationPolicy(
            int batchSize,
            Duration claimLease,
            Duration initialRetryDelay,
            Duration maximumRetryDelay
    ) {

        public PublicationPolicy {
            if (batchSize <= 0
                    || claimLease == null
                    || claimLease.isZero()
                    || claimLease.isNegative()
                    || initialRetryDelay == null
                    || initialRetryDelay.isZero()
                    || initialRetryDelay.isNegative()
                    || maximumRetryDelay == null
                    || maximumRetryDelay.compareTo(initialRetryDelay) < 0) {
                throw new IllegalArgumentException("Outbox 발행 정책이 유효하지 않습니다.");
            }
        }
    }
}
