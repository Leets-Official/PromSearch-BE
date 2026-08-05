package com.promsearch.worker.prompt.infrastructure.sqs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promsearch.prompt.application.usecase.ProcessPromptImageWatermarkUseCase;
import com.promsearch.prompt.application.usecase.dto.PromptImageWatermarkJob;
import com.promsearch.prompt.infrastructure.messaging.sqs.WatermarkSqsProperties;
import com.promsearch.worker.prompt.infrastructure.image.WatermarkRenderingProperties;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

/**
 * SQS 작업을 Long Polling으로 받고 성공한 메시지만 삭제하는 Worker 진입점.
 *
 * <p>처리 실패나 역직렬화 실패 메시지는 삭제하지 않아 Visibility Timeout 이후
 * 재시도되며, 큐의 Redrive Policy 횟수를 넘으면 DLQ로 이동한다.</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "messaging.sqs.watermark.enabled",
        havingValue = "true"
)
public class SqsPromptImageWatermarkJobConsumer {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final ProcessPromptImageWatermarkUseCase processWatermarkUseCase;
    private final WatermarkSqsProperties properties;
    private final WatermarkRenderingProperties renderingProperties;
    private final ExecutorService watermarkTaskExecutor;
    private final Semaphore availableProcessingSlots;
    private final AtomicBoolean connectivityConfirmed = new AtomicBoolean();

    public SqsPromptImageWatermarkJobConsumer(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            ProcessPromptImageWatermarkUseCase processWatermarkUseCase,
            WatermarkSqsProperties properties,
            WatermarkRenderingProperties renderingProperties,
            ExecutorService watermarkTaskExecutor
    ) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.processWatermarkUseCase = processWatermarkUseCase;
        this.properties = properties;
        this.renderingProperties = renderingProperties;
        this.watermarkTaskExecutor = watermarkTaskExecutor;
        this.availableProcessingSlots = new Semaphore(renderingProperties.concurrency());
    }

    /** 한 번의 Long Polling이 끝난 후 짧게 쉬고 다음 메시지를 조회 */
    @Scheduled(
            fixedDelayString =
                    "${messaging.sqs.watermark.worker-poll-delay-milliseconds:200}"
    )
    public void pollAvailableMessage() {
        int reservedSlots = reserveAvailableSlots();
        if (reservedSlots == 0) {
            return;
        }

        List<Message> messages;
        try {
            messages = receiveMessages(reservedSlots);
            if (connectivityConfirmed.compareAndSet(false, true)) {
                log.info("prompt_image_watermark_sqs_poll_ready");
            }
        } catch (RuntimeException exception) {
            availableProcessingSlots.release(reservedSlots);
            log.warn("prompt_image_watermark_sqs_poll_failed", exception);
            return;
        }

        availableProcessingSlots.release(reservedSlots - messages.size());
        messages.forEach(this::submitMessage);
    }

    /** 실행기 슬롯을 선점한 수만큼만 SQS에서 가져와 visibility timeout 중 대기를 방지 */
    private List<Message> receiveMessages(int maximumMessages) {
        return sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                        .queueUrl(properties.queueUrl())
                        .maxNumberOfMessages(maximumMessages)
                        .waitTimeSeconds(properties.receiveWaitTimeSeconds())
                        .visibilityTimeout(properties.visibilityTimeoutSeconds())
                        .messageSystemAttributeNames(
                                MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT
                        )
                        .build())
                .messages();
    }

    private int reserveAvailableSlots() {
        int maximumMessages = Math.min(renderingProperties.concurrency(), 10);
        int reservedSlots = 0;
        while (reservedSlots < maximumMessages && availableProcessingSlots.tryAcquire()) {
            reservedSlots++;
        }
        return reservedSlots;
    }

    private void submitMessage(Message message) {
        try {
            watermarkTaskExecutor.execute(() -> {
                try {
                    processMessage(message);
                } finally {
                    availableProcessingSlots.release();
                }
            });
        } catch (RuntimeException exception) {
            availableProcessingSlots.release();
            log.warn("prompt_image_watermark_task_submission_failed messageId={}",
                    message.messageId(), exception);
        }
    }

    /** JSON 작업 처리와 결과 저장까지 성공한 경우에만 SQS 메시지를 삭제 */
    private void processMessage(Message message) {
        String receiveCount = message.attributes().getOrDefault(
                MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT,
                "unknown"
        );
        try {
            PromptImageWatermarkJob job =
                    objectMapper.readValue(message.body(), PromptImageWatermarkJob.class);
            processWatermarkUseCase.process(job);
            deleteMessage(message.receiptHandle());
            log.info(
                    "prompt_image_watermark_sqs_message_completed "
                            + "messageId={} imageId={} receiveCount={}",
                    message.messageId(),
                    job.imageId(),
                    receiveCount
            );
        } catch (JsonProcessingException exception) {
            log.error(
                    "prompt_image_watermark_sqs_message_invalid "
                            + "messageId={} receiveCount={}",
                    message.messageId(),
                    receiveCount,
                    exception
            );
        } catch (RuntimeException exception) {
            log.warn(
                    "prompt_image_watermark_sqs_message_failed "
                            + "messageId={} receiveCount={}",
                    message.messageId(),
                    receiveCount,
                    exception
            );
        }
    }

    /** 처리 완료 메시지를 receipt handle로 삭제해 재수신을 방지 */
    private void deleteMessage(String receiptHandle) {
        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(properties.queueUrl())
                .receiptHandle(receiptHandle)
                .build());
    }
}
