package com.promsearch.prompt.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promsearch.global.config.JpaConfig;
import com.promsearch.prompt.application.usecase.dto.PendingPromptImageWatermarkMessage;
import com.promsearch.prompt.application.usecase.dto.PromptImageWatermarkJob;
import com.promsearch.prompt.domain.PromptImage;
import com.promsearch.prompt.domain.enums.PromptImageContentType;
import com.promsearch.prompt.infrastructure.persistence.entity.PromptImageJpaEntity;
import com.promsearch.prompt.infrastructure.persistence.entity.PromptImageWatermarkOutboxJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
        JpaConfig.class,
        PromptImageWatermarkOutboxPublicationAdapter.class
})
class PromptImageWatermarkOutboxPublicationAdapterTest {

    private static final Instant OCCURRED_AT = Instant.parse("2026-07-27T12:00:00Z");

    @Autowired
    private PromptImageRepository imageRepository;
    @Autowired
    private PromptImageWatermarkOutboxRepository outboxRepository;
    @Autowired
    private PromptImageWatermarkOutboxPublicationAdapter adapter;

    @BeforeEach
    void cleanDatabase() {
        outboxRepository.deleteAll();
        imageRepository.deleteAll();
    }

    @DisplayName("발행 가능한 작업을 선점하면 임대 만료 전에는 다시 조회되지 않는다")
    @Test
    void claimAvailableMessageWithLease() throws Exception {
        PromptImageWatermarkOutboxJpaEntity outbox = savePendingOutbox();

        List<PendingPromptImageWatermarkMessage> first = adapter.claimAvailable(
                OCCURRED_AT,
                OCCURRED_AT.plusSeconds(30),
                10
        );
        List<PendingPromptImageWatermarkMessage> second = adapter.claimAvailable(
                OCCURRED_AT.plusSeconds(10),
                OCCURRED_AT.plusSeconds(40),
                10
        );

        assertThat(first).singleElement()
                .extracting(PendingPromptImageWatermarkMessage::eventId)
                .isEqualTo(outbox.getEventId());
        assertThat(second).isEmpty();
    }

    @DisplayName("발행 실패 작업은 재시도 시각 이후 다시 선점할 수 있다")
    @Test
    void rescheduleFailedMessage() throws Exception {
        PromptImageWatermarkOutboxJpaEntity outbox = savePendingOutbox();
        adapter.claimAvailable(OCCURRED_AT, OCCURRED_AT.plusSeconds(30), 10);

        adapter.reschedule(
                outbox.getEventId(),
                OCCURRED_AT.plusSeconds(20),
                "일시적인 전송 실패"
        );

        assertThat(adapter.claimAvailable(
                OCCURRED_AT.plusSeconds(19),
                OCCURRED_AT.plusSeconds(49),
                10
        )).isEmpty();
        assertThat(adapter.claimAvailable(
                OCCURRED_AT.plusSeconds(20),
                OCCURRED_AT.plusSeconds(50),
                10
        )).singleElement()
                .extracting(PendingPromptImageWatermarkMessage::attemptCount)
                .isEqualTo(1);
    }

    @DisplayName("발행 완료 작업은 이후 발행 대상에서 제외된다")
    @Test
    void excludePublishedMessage() throws Exception {
        PromptImageWatermarkOutboxJpaEntity outbox = savePendingOutbox();
        adapter.claimAvailable(OCCURRED_AT, OCCURRED_AT.plusSeconds(30), 10);

        adapter.markPublished(outbox.getEventId(), OCCURRED_AT.plusSeconds(1));

        PromptImageWatermarkOutboxJpaEntity saved =
                outboxRepository.findById(outbox.getEventId()).orElseThrow();
        assertThat(saved.getStatus())
                .isEqualTo(PromptImageWatermarkOutboxJpaEntity.Status.PUBLISHED);
        assertThat(saved.getPublishedAt()).isEqualTo(OCCURRED_AT.plusSeconds(1));
        assertThat(adapter.claimAvailable(
                OCCURRED_AT.plusSeconds(31),
                OCCURRED_AT.plusSeconds(61),
                10
        )).isEmpty();
    }

    private PromptImageWatermarkOutboxJpaEntity savePendingOutbox() throws Exception {
        UUID imageId = UUID.randomUUID();
        PromptImage image = PromptImage.prepareUpload(
                imageId,
                1L,
                "prompt-images/original/1/" + imageId + ".png",
                "result.png",
                PromptImageContentType.PNG,
                1_024,
                640,
                360
        );
        imageRepository.saveAndFlush(PromptImageJpaEntity.from(image));

        PromptImageWatermarkJob job = new PromptImageWatermarkJob(
                PromptImageWatermarkJob.CURRENT_EVENT_VERSION,
                UUID.randomUUID(),
                imageId,
                image.getOriginalObjectKey(),
                "prompt-images/watermarked/1/" + imageId + ".png",
                "image/png",
                1,
                OCCURRED_AT
        );
        PromptImageWatermarkOutboxJpaEntity outbox =
                PromptImageWatermarkOutboxJpaEntity.pending(
                        job,
                        new ObjectMapper().findAndRegisterModules().writeValueAsString(job)
                );
        return outboxRepository.saveAndFlush(outbox);
    }
}
