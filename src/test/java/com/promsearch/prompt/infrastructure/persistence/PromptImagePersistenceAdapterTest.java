package com.promsearch.prompt.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.promsearch.global.config.JpaConfig;
import com.promsearch.prompt.domain.PromptImage;
import com.promsearch.prompt.domain.enums.PromptImageContentType;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import com.promsearch.prompt.infrastructure.persistence.entity.PromptImageJpaEntity;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaConfig.class)
class PromptImagePersistenceAdapterTest {

    @Autowired
    private PromptImageRepository promptImageRepository;

    private PromptImagePersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PromptImagePersistenceAdapter(promptImageRepository);
    }

    @DisplayName("이미지 식별자 목록으로 도메인 이미지 자산을 일괄 조회한다")
    @Test
    void listByIdsReturnsPromptImageDomains() {
        PromptImage first = prepareImage(1L, "first.jpg");
        PromptImage second = prepareImage(1L, "second.jpg");
        PromptImage excluded = prepareImage(1L, "excluded.jpg");
        promptImageRepository.saveAll(List.of(
                PromptImageJpaEntity.from(first),
                PromptImageJpaEntity.from(second),
                PromptImageJpaEntity.from(excluded)
        ));
        promptImageRepository.flush();

        List<PromptImage> images = adapter.listByIds(List.of(
                first.getPromptImageId().id(),
                second.getPromptImageId().id()
        ));

        assertThat(images)
                .extracting(image -> image.getPromptImageId().id())
                .containsExactlyInAnyOrder(first.getPromptImageId().id(), second.getPromptImageId().id());
        assertThat(images)
                .extracting(PromptImage::getOriginalFileName)
                .containsExactlyInAnyOrder("first.jpg", "second.jpg");
    }

    @DisplayName("일괄 조회 요청에 null 식별자가 있으면 거절한다")
    @Test
    void listByIdsRejectsNullImageId() {
        assertThatThrownBy(() -> adapter.listByIds(java.util.Arrays.asList(UUID.randomUUID(), null)))
                .isInstanceOf(PromptDomainException.class)
                .extracting("baseCode")
                .isEqualTo(PromptErrorCode.INVALID_ID);
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
