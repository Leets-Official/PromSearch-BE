package com.promsearch.prompt.infrastructure.messaging.sqs;

import com.promsearch.prompt.application.port.out.promptimage.PublishPromptImageWatermarkJobPort;
import com.promsearch.prompt.application.usecase.dto.PendingPromptImageWatermarkMessage;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/** Outbox JSON 메시지를 AWS SQS 표준 큐에 전송 */
@RequiredArgsConstructor
public class SqsPromptImageWatermarkJobPublisher
        implements PublishPromptImageWatermarkJobPort {

    private final SqsClient sqsClient;
    private final WatermarkSqsProperties properties;

    /** 이벤트 식별자를 메시지 속성에 함께 기록하고 JSON 본문을 그대로 발행 */
    @Override
    public void publish(PendingPromptImageWatermarkMessage message) {
        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(properties.queueUrl())
                .messageBody(message.payload())
                .messageAttributes(Map.of(
                        "eventId",
                        MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(message.eventId().toString())
                                .build()
                ))
                .build());
    }
}
