package com.promsearch.prompt.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.promsearch.global.config.JpaConfig;
import com.promsearch.prompt.application.port.out.storage.DeletePromptImageObjectPort;
import com.promsearch.prompt.application.port.out.storage.GeneratePromptImageObjectKeyPort;
import com.promsearch.prompt.application.port.out.storage.LoadPromptImageObjectMetadataPort;
import com.promsearch.prompt.application.port.out.storage.LoadPromptImageObjectMetadataPort.StoredObjectMetadata;
import com.promsearch.prompt.application.port.out.storage.PresignPromptImageUploadPort;
import com.promsearch.prompt.application.usecase.dto.CompletePromptImageUploadCommand;
import com.promsearch.prompt.application.usecase.dto.PromptImageWatermarkJob;
import com.promsearch.prompt.domain.PromptImage;
import com.promsearch.prompt.domain.enums.PromptImageContentType;
import com.promsearch.prompt.domain.enums.PromptImageStatus;
import com.promsearch.prompt.infrastructure.persistence.PromptImagePersistenceAdapter;
import com.promsearch.prompt.infrastructure.persistence.PromptImageRepository;
import com.promsearch.prompt.infrastructure.persistence.PromptImageWatermarkOutboxPersistenceAdapter;
import com.promsearch.prompt.infrastructure.persistence.PromptImageWatermarkOutboxRepository;
import com.promsearch.prompt.infrastructure.persistence.entity.PromptImageJpaEntity;
import com.promsearch.prompt.infrastructure.persistence.entity.PromptImageWatermarkOutboxJpaEntity;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
        JpaConfig.class,
        PromptImagePersistenceAdapter.class,
        PromptImageWatermarkOutboxPersistenceAdapter.class,
        PromptImageUploadCommandService.class,
        PromptImageUploadOutboxIntegrationTest.ObjectMapperTestConfig.class
})
class PromptImageUploadOutboxIntegrationTest {

    @Autowired
    private PromptImageUploadCommandService service;
    @Autowired
    private PromptImageRepository promptImageRepository;
    @Autowired
    private PromptImageWatermarkOutboxRepository outboxRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GeneratePromptImageObjectKeyPort generatePromptImageObjectKeyPort;
    @MockitoBean
    private PresignPromptImageUploadPort presignPromptImageUploadPort;
    @MockitoBean
    private LoadPromptImageObjectMetadataPort loadPromptImageObjectMetadataPort;
    @MockitoBean
    private DeletePromptImageObjectPort deletePromptImageObjectPort;

    @BeforeEach
    void cleanDatabase() {
        outboxRepository.deleteAll();
        promptImageRepository.deleteAll();
    }

    @DisplayName("업로드 완료 상태와 워터마크 Outbox 작업을 한 트랜잭션에 저장한다")
    @Test
    void completeUploadStoresImageAndOutboxTogether() throws Exception {
        PromptImage image = saveUploadingImage();
        String watermarkedObjectKey = watermarkedObjectKey(image);
        arrangeCompletion(image, watermarkedObjectKey);

        service.complete(new CompletePromptImageUploadCommand(
                image.getUploaderId(),
                image.getPromptImageId().id()
        ));

        PromptImage savedImage = promptImageRepository.findById(image.getPromptImageId().id())
                .orElseThrow()
                .toDomain();
        PromptImageWatermarkOutboxJpaEntity outbox = outboxRepository.findAll().getFirst();
        PromptImageWatermarkJob job =
                objectMapper.readValue(outbox.getPayload(), PromptImageWatermarkJob.class);

        assertThat(savedImage.getStatus()).isEqualTo(PromptImageStatus.UPLOADED);
        assertThat(outbox.getStatus())
                .isEqualTo(PromptImageWatermarkOutboxJpaEntity.Status.PENDING);
        assertThat(job.imageId()).isEqualTo(image.getPromptImageId().id());
        assertThat(job.watermarkedObjectKey()).isEqualTo(watermarkedObjectKey);
    }

    @DisplayName("Outbox 저장이 실패하면 이미지의 UPLOADED 상태 변경도 롤백한다")
    @Test
    void rollbackImageWhenOutboxSaveFails() throws Exception {
        PromptImage image = saveUploadingImage();
        String watermarkedObjectKey = watermarkedObjectKey(image);
        outboxRepository.saveAndFlush(PromptImageWatermarkOutboxJpaEntity.pending(
                existingJob(image, watermarkedObjectKey),
                "{}"
        ));
        arrangeCompletion(image, watermarkedObjectKey);

        assertThatThrownBy(() -> service.complete(new CompletePromptImageUploadCommand(
                image.getUploaderId(),
                image.getPromptImageId().id()
        ))).isInstanceOf(DataIntegrityViolationException.class);

        PromptImage rolledBackImage = promptImageRepository.findById(image.getPromptImageId().id())
                .orElseThrow()
                .toDomain();
        assertThat(rolledBackImage.getStatus()).isEqualTo(PromptImageStatus.UPLOADING);
        assertThat(outboxRepository.count()).isEqualTo(1);
    }

    private PromptImage saveUploadingImage() {
        UUID imageId = UUID.randomUUID();
        PromptImage image = PromptImage.prepareUpload(
                imageId,
                1L,
                "prompt-images/original/1/" + imageId + ".jpg",
                "result.jpg",
                PromptImageContentType.JPEG,
                1_024,
                1_920,
                1_080
        );
        promptImageRepository.saveAndFlush(PromptImageJpaEntity.from(image));
        return image;
    }

    private void arrangeCompletion(PromptImage image, String watermarkedObjectKey) {
        when(loadPromptImageObjectMetadataPort.getMetadata(image.getOriginalObjectKey()))
                .thenReturn(new StoredObjectMetadata(
                        image.getFileSize(),
                        image.getContentType().getMimeType(),
                        "\"etag\"",
                        Instant.parse("2026-07-27T00:00:00Z")
                ));
        when(generatePromptImageObjectKeyPort.generateWatermarked(
                image.getUploaderId(),
                image.getPromptImageId().id(),
                image.getContentType()
        )).thenReturn(watermarkedObjectKey);
    }

    private PromptImageWatermarkJob existingJob(
            PromptImage image,
            String watermarkedObjectKey
    ) {
        return new PromptImageWatermarkJob(
                PromptImageWatermarkJob.CURRENT_EVENT_VERSION,
                UUID.randomUUID(),
                image.getPromptImageId().id(),
                image.getOriginalObjectKey(),
                watermarkedObjectKey,
                image.getContentType().getMimeType(),
                1,
                Instant.parse("2026-07-27T00:00:00Z")
        );
    }

    private String watermarkedObjectKey(PromptImage image) {
        return "prompt-images/watermarked/1/" + image.getPromptImageId().id() + ".jpg";
    }

    @TestConfiguration
    static class ObjectMapperTestConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().registerModule(new JavaTimeModule());
        }
    }
}
