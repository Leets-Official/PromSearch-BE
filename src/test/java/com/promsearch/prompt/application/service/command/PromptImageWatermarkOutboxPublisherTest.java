package com.promsearch.prompt.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.promsearch.prompt.application.port.out.promptimage.ClaimPromptImageWatermarkJobsPort;
import com.promsearch.prompt.application.port.out.promptimage.PublishPromptImageWatermarkJobPort;
import com.promsearch.prompt.application.port.out.promptimage.UpdatePromptImageWatermarkOutboxPort;
import com.promsearch.prompt.application.service.command.PromptImageWatermarkOutboxPublisher.PublicationPolicy;
import com.promsearch.prompt.application.usecase.PublishPendingPromptImageWatermarkJobsUseCase.PublicationResult;
import com.promsearch.prompt.application.usecase.dto.PendingPromptImageWatermarkMessage;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PromptImageWatermarkOutboxPublisherTest {

    private static final Instant NOW = Instant.parse("2026-07-27T12:00:00Z");

    @Mock
    private ClaimPromptImageWatermarkJobsPort claimJobsPort;
    @Mock
    private PublishPromptImageWatermarkJobPort publishJobPort;
    @Mock
    private UpdatePromptImageWatermarkOutboxPort updateOutboxPort;

    private PromptImageWatermarkOutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new PromptImageWatermarkOutboxPublisher(
                claimJobsPort,
                publishJobPort,
                updateOutboxPort,
                new PublicationPolicy(
                        20,
                        Duration.ofSeconds(30),
                        Duration.ofSeconds(5),
                        Duration.ofMinutes(5)
                ),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @DisplayName("선점한 작업을 발행한 뒤 성공 상태를 기록한다")
    @Test
    void publishClaimedMessage() {
        PendingPromptImageWatermarkMessage message = message(0);
        when(claimJobsPort.claimAvailable(
                NOW,
                NOW.plusSeconds(30),
                20
        )).thenReturn(List.of(message));

        PublicationResult result = publisher.publishAvailable();

        assertThat(result).isEqualTo(new PublicationResult(1, 1, 0));
        InOrder order = inOrder(publishJobPort, updateOutboxPort);
        order.verify(publishJobPort).publish(message);
        order.verify(updateOutboxPort).markPublished(message.eventId(), NOW);
    }

    @DisplayName("발행 실패는 지수 백오프가 적용된 다음 시도 시각과 오류를 기록한다")
    @Test
    void rescheduleFailedMessageWithExponentialBackoff() {
        PendingPromptImageWatermarkMessage message = message(2);
        when(claimJobsPort.claimAvailable(
                NOW,
                NOW.plusSeconds(30),
                20
        )).thenReturn(List.of(message));
        doThrow(new IllegalStateException("SQS 전송 실패"))
                .when(publishJobPort)
                .publish(message);

        PublicationResult result = publisher.publishAvailable();

        assertThat(result).isEqualTo(new PublicationResult(1, 0, 1));
        verify(updateOutboxPort).reschedule(
                message.eventId(),
                NOW.plusSeconds(20),
                "SQS 전송 실패"
        );
    }

    @DisplayName("발행할 작업이 없으면 외부 메시지 전송을 호출하지 않는다")
    @Test
    void skipWhenNoMessageAvailable() {
        when(claimJobsPort.claimAvailable(
                NOW,
                NOW.plusSeconds(30),
                20
        )).thenReturn(List.of());

        PublicationResult result = publisher.publishAvailable();

        assertThat(result).isEqualTo(new PublicationResult(0, 0, 0));
    }

    private PendingPromptImageWatermarkMessage message(int attemptCount) {
        return new PendingPromptImageWatermarkMessage(
                UUID.randomUUID(),
                "{\"eventVersion\":1}",
                attemptCount
        );
    }
}
