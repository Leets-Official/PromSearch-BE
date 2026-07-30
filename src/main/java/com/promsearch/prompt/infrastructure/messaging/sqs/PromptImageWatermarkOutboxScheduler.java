package com.promsearch.prompt.infrastructure.messaging.sqs;

import com.promsearch.prompt.application.usecase.PublishPendingPromptImageWatermarkJobsUseCase;
import com.promsearch.prompt.application.usecase.PublishPendingPromptImageWatermarkJobsUseCase.PublicationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

/** 일정 주기로 커밋된 Outbox 작업을 SQS에 전달 */
@Slf4j
@RequiredArgsConstructor
public class PromptImageWatermarkOutboxScheduler {

    private final PublishPendingPromptImageWatermarkJobsUseCase publishJobsUseCase;

    /** 한 번에 한 배치를 발행하고 개별 실패는 Outbox 백오프 정책에 위임 */
    @Scheduled(
            fixedDelayString =
                    "${messaging.sqs.watermark.publisher-poll-delay-milliseconds:1000}"
    )
    public void publishAvailable() {
        try {
            PublicationResult result = publishJobsUseCase.publishAvailable();
            if (result.claimedCount() > 0) {
                log.info(
                        "prompt_image_watermark_outbox_published claimedCount={} "
                                + "publishedCount={} failedCount={}",
                        result.claimedCount(),
                        result.publishedCount(),
                        result.failedCount()
                );
            }
        } catch (RuntimeException exception) {
            log.error("prompt_image_watermark_outbox_batch_failed", exception);
        }
    }
}
