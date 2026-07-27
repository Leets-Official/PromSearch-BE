package com.promsearch.prompt.infrastructure.messaging.sqs;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** API 발행기와 Worker 소비기가 공유하는 워터마크 SQS 설정 */
@Validated
@ConfigurationProperties(prefix = "messaging.sqs.watermark")
public record WatermarkSqsProperties(
        boolean enabled,
        String queueUrl,
        int publisherBatchSize,
        long publisherPollDelayMilliseconds,
        Duration claimLease,
        Duration initialRetryDelay,
        Duration maximumRetryDelay,
        long workerPollDelayMilliseconds,
        int receiveWaitTimeSeconds,
        int visibilityTimeoutSeconds
) {

    public WatermarkSqsProperties {
        if (enabled) {
            if (queueUrl == null || queueUrl.isBlank()) {
                throw new IllegalArgumentException("워터마크 SQS 큐 URL은 필수입니다.");
            }
            if (publisherBatchSize <= 0
                    || publisherBatchSize > 100
                    || publisherPollDelayMilliseconds <= 0
                    || !isPositive(claimLease)
                    || !isPositive(initialRetryDelay)
                    || maximumRetryDelay == null
                    || maximumRetryDelay.compareTo(initialRetryDelay) < 0
                    || workerPollDelayMilliseconds <= 0
                    || receiveWaitTimeSeconds < 1
                    || receiveWaitTimeSeconds > 20
                    || visibilityTimeoutSeconds <= receiveWaitTimeSeconds
                    || visibilityTimeoutSeconds > 43_200) {
                throw new IllegalArgumentException("워터마크 SQS 설정이 유효하지 않습니다.");
            }
        }
    }

    /** 설정 시간이 null이 아니며 실제 대기 시간을 나타내는 양수인지 확인 */
    private static boolean isPositive(Duration duration) {
        return duration != null && !duration.isZero() && !duration.isNegative();
    }
}
