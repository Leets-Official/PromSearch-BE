package com.promsearch.worker.prompt.infrastructure.sqs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.promsearch.prompt.application.usecase.ProcessPromptImageWatermarkUseCase;
import com.promsearch.prompt.application.usecase.dto.PromptImageWatermarkJob;
import com.promsearch.prompt.infrastructure.messaging.sqs.WatermarkSqsProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

class SqsPromptImageWatermarkJobConsumerTest {

    private static final String QUEUE_URL =
            "https://sqs.ap-northeast-2.amazonaws.com/123456789012/watermark";

    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new JavaTimeModule());

    private SqsClient sqsClient;
    private ProcessPromptImageWatermarkUseCase processUseCase;
    private SqsPromptImageWatermarkJobConsumer consumer;

    @BeforeEach
    void setUp() {
        sqsClient = mock(SqsClient.class);
        processUseCase = mock(ProcessPromptImageWatermarkUseCase.class);
        consumer = new SqsPromptImageWatermarkJobConsumer(
                sqsClient,
                objectMapper,
                processUseCase,
                properties()
        );
    }

    @DisplayName("워터마크 작업 성공 후 SQS 메시지를 삭제한다")
    @Test
    void deleteMessageAfterSuccessfulProcessing() throws Exception {
        PromptImageWatermarkJob job = job();
        Message message = message(objectMapper.writeValueAsString(job));
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(response(message));

        consumer.pollAvailableMessage();

        verify(processUseCase).process(job);
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @DisplayName("워터마크 작업 실패 시 재시도를 위해 SQS 메시지를 삭제하지 않는다")
    @Test
    void retainMessageWhenProcessingFails() throws Exception {
        PromptImageWatermarkJob job = job();
        Message message = message(objectMapper.writeValueAsString(job));
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(response(message));
        doThrow(new IllegalStateException("이미지 처리 실패"))
                .when(processUseCase)
                .process(job);

        consumer.pollAvailableMessage();

        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @DisplayName("잘못된 메시지는 처리하거나 삭제하지 않아 DLQ 이동 대상으로 남긴다")
    @Test
    void retainInvalidMessage() {
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(response(message("{invalid-json")));

        consumer.pollAvailableMessage();

        verify(processUseCase, never()).process(any(PromptImageWatermarkJob.class));
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    /** Worker 소비기 테스트에 필요한 유효한 기본 설정 생성 */
    private WatermarkSqsProperties properties() {
        return new WatermarkSqsProperties(
                true,
                QUEUE_URL,
                10,
                1_000,
                Duration.ofSeconds(60),
                Duration.ofSeconds(5),
                Duration.ofMinutes(5),
                200,
                20,
                120
        );
    }

    /** 직렬화와 도메인 검증을 통과하는 워터마크 작업 생성 */
    private PromptImageWatermarkJob job() {
        return new PromptImageWatermarkJob(
                PromptImageWatermarkJob.CURRENT_EVENT_VERSION,
                UUID.fromString("08f4bba0-40a7-4bb4-a847-8dd3645bd9c7"),
                UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                "prompt-images/original/10/123e4567-e89b-12d3-a456-426614174000.png",
                "prompt-images/watermarked/10/123e4567-e89b-12d3-a456-426614174000.png",
                "image/png",
                1,
                Instant.parse("2026-07-27T00:00:00Z")
        );
    }

    /** 수신 횟수와 receipt handle을 포함한 SQS 메시지 생성 */
    private Message message(String body) {
        return Message.builder()
                .messageId("message-id")
                .receiptHandle("receipt-handle")
                .body(body)
                .attributes(Map.of(
                        MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT,
                        "1"
                ))
                .build();
    }

    /** SQS 단일 메시지 수신 응답 생성 */
    private ReceiveMessageResponse response(Message message) {
        return ReceiveMessageResponse.builder()
                .messages(List.of(message))
                .build();
    }
}
