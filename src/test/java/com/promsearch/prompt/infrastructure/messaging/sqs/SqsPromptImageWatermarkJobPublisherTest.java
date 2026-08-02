package com.promsearch.prompt.infrastructure.messaging.sqs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.promsearch.prompt.application.usecase.dto.PendingPromptImageWatermarkMessage;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

class SqsPromptImageWatermarkJobPublisherTest {

    private static final String QUEUE_URL =
            "https://sqs.ap-northeast-2.amazonaws.com/123456789012/watermark";

    private SqsClient sqsClient;
    private SqsPromptImageWatermarkJobPublisher publisher;

    @BeforeEach
    void setUp() {
        sqsClient = mock(SqsClient.class);
        publisher = new SqsPromptImageWatermarkJobPublisher(
                sqsClient,
                properties()
        );
    }

    @DisplayName("Outbox 메시지 본문과 이벤트 식별자를 SQS에 전송한다")
    @Test
    void publishMessage() {
        UUID eventId = UUID.randomUUID();
        PendingPromptImageWatermarkMessage message =
                new PendingPromptImageWatermarkMessage(
                        eventId,
                        "{\"eventVersion\":1}",
                        0
                );

        publisher.publish(message);

        ArgumentCaptor<SendMessageRequest> requestCaptor =
                ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(requestCaptor.capture());
        SendMessageRequest request = requestCaptor.getValue();
        assertThat(request.queueUrl()).isEqualTo(QUEUE_URL);
        assertThat(request.messageBody()).isEqualTo(message.payload());
        assertThat(request.messageAttributes().get("eventId").stringValue())
                .isEqualTo(eventId.toString());
    }

    /** SQS 발행기 테스트에 필요한 유효한 기본 설정 생성 */
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
}
