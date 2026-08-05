package com.promsearch.worker.prompt.infrastructure.sqs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promsearch.prompt.application.usecase.ProcessPromptImageWatermarkUseCase;
import com.promsearch.prompt.application.usecase.dto.PromptImageWatermarkJob;
import com.promsearch.prompt.infrastructure.messaging.sqs.WatermarkSqsProperties;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "messaging.sqs.watermark.enabled",
        havingValue = "true"
)
public class SqsPromptImageWatermarkJobConsumer {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final ProcessPromptImageWatermarkUseCase processWatermarkUseCase;
    private final WatermarkSqsProperties properties;
    private final AtomicBoolean connectivityConfirmed = new AtomicBoolean();

    /** 한 번의 Long Polling이 끝난 후 짧게 쉬고 다음 메시지를 조회 */
    @Scheduled(
            fixedDelayString =
                    "${messaging.sqs.watermark.worker-poll-delay-milliseconds:200}"
    )
    public void pollAvailableMessage() {
        try {
            List<Message> messages = receiveMessages();
            if (connectivityConfirmed.compareAndSet(false, true)) {
                log.info("prompt_image_watermark_sqs_poll_ready");
            }
            messages.forEach(this::processMessage);
        } catch (RuntimeException exception) {
            log.warn("prompt_image_watermark_sqs_poll_failed", exception);
        }
    }

    /** 현재는 이미지 메모리 사용량을 제한하기 위해 한 번에 한 메시지만 수신 */
    private List<Message> receiveMessages() {
        // TODO: Worker 병렬 처리 도입 시 maxNumberOfMessages와 실행 풀 크기를 함께 늘리고,
        // heap 사용량·GC pause·S3 대역폭·DB 풀 포화도를 기준으로 상한 결정
        return sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                        .queueUrl(properties.queueUrl())
                        .maxNumberOfMessages(1)
                        .waitTimeSeconds(properties.receiveWaitTimeSeconds())
                        .visibilityTimeout(properties.visibilityTimeoutSeconds())
                        .messageSystemAttributeNames(
                                MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT
                        )
                        .build())
                .messages();
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
