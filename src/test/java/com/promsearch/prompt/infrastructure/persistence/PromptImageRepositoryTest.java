package com.promsearch.prompt.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.promsearch.global.config.JpaConfig;
import com.promsearch.prompt.domain.PromptImage;
import com.promsearch.prompt.domain.enums.PromptImageContentType;
import com.promsearch.prompt.domain.enums.PromptImageStatus;
import com.promsearch.prompt.infrastructure.persistence.entity.PromptImageJpaEntity;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({JpaConfig.class, PromptImagePersistenceAdapter.class})
class PromptImageRepositoryTest {

    @Autowired
    private PromptImageRepository promptImageRepository;

    @Autowired
    private PromptImagePersistenceAdapter promptImagePersistenceAdapter;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("UUID 이미지 자산과 처리 상태를 저장하고 다시 도메인으로 복원한다")
    @Test
    void saveAndReconstructPromptImage() {
        PromptImage image = prepareImage(1L, "first.jpg");
        PromptImageJpaEntity entity = promptImageRepository.saveAndFlush(PromptImageJpaEntity.from(image));
        image.completeUpload("\"etag\"", Instant.parse("2026-07-26T01:00:00Z"));
        image.startProcessing();
        image.completeProcessing("watermarked/1/" + image.getPromptImageId().id() + ".jpg", 1);
        entity.updateFrom(image);
        promptImageRepository.flush();
        entityManager.clear();

        PromptImage savedImage = promptImageRepository.findById(image.getPromptImageId().id())
                .orElseThrow()
                .toDomain();

        assertThat(savedImage.getPromptImageId()).isEqualTo(image.getPromptImageId());
        assertThat(savedImage.getUploaderId()).isEqualTo(1L);
        assertThat(savedImage.getOriginalObjectKey()).isEqualTo(image.getOriginalObjectKey());
        assertThat(savedImage.getStatus()).isEqualTo(PromptImageStatus.READY);
        assertThat(savedImage.getEtag()).isEqualTo("\"etag\"");
        assertThat(savedImage.getUploadedAt()).isEqualTo(Instant.parse("2026-07-26T01:00:00Z"));
        assertThat(savedImage.getProcessingVersion()).isEqualTo(1);
        assertThat(entity.getLockVersion()).isNotNull();
    }

    @DisplayName("프롬프트에 연결된 이미지는 정렬 순서대로 조회한다")
    @Test
    void findAttachedImagesInSortOrder() {
        PromptImage second = readyImage(1L, "second.jpg");
        second.attachToPrompt(10L, 1L, 1, false);
        PromptImage first = readyImage(1L, "first.jpg");
        first.attachToPrompt(10L, 1L, 0, true);

        promptImageRepository.saveAll(List.of(
                PromptImageJpaEntity.from(second),
                PromptImageJpaEntity.from(first)
        ));
        promptImageRepository.flush();
        entityManager.clear();

        List<PromptImageJpaEntity> images =
                promptImageRepository.findAllByPromptIdOrderBySortOrderAsc(10L);

        assertThat(images)
                .extracting(PromptImageJpaEntity::getSortOrder)
                .containsExactly(0, 1);
        assertThat(images.getFirst().getOriginalFileName()).isEqualTo("first.jpg");
        assertThat(images.getFirst().getThumbnail()).isTrue();
    }

    @DisplayName("상세 조회용 이미지 조회 시 삭제된 이미지는 제외한다")
    @Test
    void findReadyImagesExcludingSoftDeletedImages() {
        PromptImage active = readyImage(1L, "active.jpg");
        active.attachToPrompt(10L, 1L, 0, true);
        PromptImage deleted = readyImage(1L, "deleted.jpg");
        deleted.attachToPrompt(10L, 1L, 1, false);

        promptImageRepository.saveAllAndFlush(List.of(
                PromptImageJpaEntity.from(active),
                PromptImageJpaEntity.from(deleted)
        ));
        entityManager.createNativeQuery(
                        "update prompt_images set deleted_at = current_timestamp "
                                + "where prompt_image_id = :id")
                .setParameter("id", deleted.getPromptImageId().id())
                .executeUpdate();
        entityManager.clear();

        List<PromptImageJpaEntity> images = promptImageRepository
                .findAllByPromptIdAndStatusAndDeletedAtIsNullOrderBySortOrderAsc(
                        10L, PromptImageStatus.READY);

        assertThat(images)
                .extracting(PromptImageJpaEntity::getId)
                .containsExactly(active.getPromptImageId().id());
    }

    @DisplayName("업로더와 처리 상태로 이미지 자산을 조회한다")
    @Test
    void findImagesByUploaderAndStatus() {
        PromptImage uploading = prepareImage(1L, "uploading.jpg");
        PromptImage ready = readyImage(1L, "ready.jpg");

        promptImageRepository.saveAll(List.of(
                PromptImageJpaEntity.from(uploading),
                PromptImageJpaEntity.from(ready)
        ));
        promptImageRepository.flush();

        List<PromptImageJpaEntity> images =
                promptImageRepository.findAllByIdInAndUploaderIdAndStatus(
                        List.of(uploading.getPromptImageId().id(), ready.getPromptImageId().id()),
                        1L,
                        PromptImageStatus.READY
                );

        assertThat(images)
                .extracting(PromptImageJpaEntity::getId)
                .containsExactly(ready.getPromptImageId().id());
    }

    @DisplayName("프롬프트 생성은 요청 이미지 전체를 잠금 조회하고 누락을 구분한다")
    @Test
    void batchGetImagesForUpdate() {
        PromptImage first = readyImage(1L, "first.jpg");
        PromptImage second = readyImage(1L, "second.jpg");
        promptImageRepository.saveAllAndFlush(List.of(
                PromptImageJpaEntity.from(first),
                PromptImageJpaEntity.from(second)
        ));

        List<PromptImage> images = promptImagePersistenceAdapter.batchGetByIdsForUpdate(List.of(
                first.getPromptImageId().id(),
                second.getPromptImageId().id()
        ));

        assertThat(images)
                .extracting(image -> image.getPromptImageId().id())
                .containsExactlyInAnyOrder(
                        first.getPromptImageId().id(),
                        second.getPromptImageId().id()
                );
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> promptImagePersistenceAdapter.batchGetByIdsForUpdate(
                                List.of(UUID.randomUUID())
                        )
                )
                .isInstanceOf(com.promsearch.prompt.domain.exception.PromptDomainException.class)
                .extracting("baseCode")
                .isEqualTo(com.promsearch.prompt.domain.exception.PromptErrorCode.IMAGE_NOT_FOUND);
    }

    @DisplayName("이미지 상태 조회 대상 식별자만 일괄 조회한다")
    @Test
    void findAllByIdIn() {
        PromptImage first = prepareImage(1L, "first.jpg");
        PromptImage second = readyImage(1L, "second.jpg");
        PromptImage excluded = readyImage(1L, "excluded.jpg");

        promptImageRepository.saveAll(List.of(
                PromptImageJpaEntity.from(first),
                PromptImageJpaEntity.from(second),
                PromptImageJpaEntity.from(excluded)
        ));
        promptImageRepository.flush();
        entityManager.clear();

        List<PromptImageJpaEntity> images = promptImageRepository.findAllByIdIn(List.of(
                first.getPromptImageId().id(),
                second.getPromptImageId().id()
        ));

        assertThat(images)
                .extracting(PromptImageJpaEntity::getId)
                .containsExactlyInAnyOrder(first.getPromptImageId().id(), second.getPromptImageId().id());
    }

    private PromptImage readyImage(Long uploaderId, String fileName) {
        PromptImage image = prepareImage(uploaderId, fileName);
        image.completeUpload("\"etag\"", Instant.parse("2026-07-26T01:00:00Z"));
        image.startProcessing();
        image.completeProcessing("watermarked/" + uploaderId + "/" + image.getPromptImageId().id() + ".jpg", 1);
        return image;
    }

    private PromptImage prepareImage(Long uploaderId, String fileName) {
        UUID imageId = UUID.randomUUID();
        return PromptImage.prepareUpload(
                imageId,
                uploaderId,
                "originals/" + uploaderId + "/" + imageId + ".jpg",
                fileName,
                PromptImageContentType.JPEG,
                1_024,
                1_920,
                1_080
        );
    }
}
