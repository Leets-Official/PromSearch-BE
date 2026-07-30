package com.promsearch.prompt.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.promsearch.global.config.JpaConfig;
import com.promsearch.prompt.application.port.out.author.LoadPromptAuthorPort;
import com.promsearch.prompt.application.port.out.pricing.LoadPromptPricingPort;
import com.promsearch.prompt.application.usecase.dto.CreatePromptCommand;
import com.promsearch.prompt.application.usecase.dto.CreatePromptCommand.ImageReference;
import com.promsearch.prompt.domain.PromptImage;
import com.promsearch.prompt.domain.Tag;
import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptImageContentType;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import com.promsearch.prompt.domain.enums.PromptVisibility;
import com.promsearch.prompt.domain.enums.TagType;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import com.promsearch.prompt.infrastructure.persistence.PostRepository;
import com.promsearch.prompt.infrastructure.persistence.PromptImagePersistenceAdapter;
import com.promsearch.prompt.infrastructure.persistence.PromptImageRepository;
import com.promsearch.prompt.infrastructure.persistence.PromptPersistenceAdapter;
import com.promsearch.prompt.infrastructure.persistence.TagPersistenceAdapter;
import com.promsearch.prompt.infrastructure.persistence.TagRepository;
import com.promsearch.prompt.infrastructure.persistence.entity.PromptImageJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
        JpaConfig.class,
        PromptCommandService.class,
        PromptPersistenceAdapter.class,
        TagPersistenceAdapter.class,
        PromptImagePersistenceAdapter.class
})
class PromptCreationTransactionIntegrationTest {

    @Autowired
    private PromptCommandService service;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PromptImageRepository promptImageRepository;

    @Autowired
    private TagPersistenceAdapter tagPersistenceAdapter;

    @Autowired
    private TagRepository tagRepository;

    @MockitoBean
    private LoadPromptAuthorPort loadPromptAuthorPort;

    @MockitoBean
    private LoadPromptPricingPort loadPromptPricingPort;

    @BeforeEach
    void cleanDatabase() {
        promptImageRepository.deleteAll();
        postRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @DisplayName("이미지 연결이 실패하면 먼저 저장한 프롬프트와 초기 연관 데이터도 롤백한다")
    @Test
    void rollbackPromptWhenImageAttachmentFails() {
        PromptImage otherUsersImage = saveReadyImage(2L);
        Tag job = tagPersistenceAdapter.create(Tag.create(TagType.JOB, "개발", "개발", false));
        Tag task = tagPersistenceAdapter.create(Tag.create(TagType.TASK, "요약", "요약", false));
        Tag aiModel = tagPersistenceAdapter.create(Tag.create(TagType.AI_MODEL, "GPT", "gpt", false));
        CreatePromptCommand command = new CreatePromptCommand(
                1L,
                "프롬프트 제목",
                "설명",
                PromptOutputType.IMAGE,
                List.of(job.getTagId().id()),
                List.of(task.getTagId().id()),
                List.of(aiModel.getTagId().id()),
                null,
                PromptContentType.FREE,
                "프롬프트 본문",
                PromptVisibility.PUBLIC,
                List.of(new ImageReference(
                        otherUsersImage.getPromptImageId().id(),
                        0,
                        true
                ))
        );

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(PromptDomainException.class)
                .extracting("baseCode")
                .isEqualTo(PromptErrorCode.IMAGE_NOT_OWNED);

        assertThat(postRepository.count()).isZero();
        PromptImage rolledBackImage = promptImageRepository.findById(
                        otherUsersImage.getPromptImageId().id()
                )
                .orElseThrow()
                .toDomain();
        assertThat(rolledBackImage.getPromptId()).isNull();
    }

    private PromptImage saveReadyImage(Long uploaderId) {
        UUID imageId = UUID.randomUUID();
        PromptImage image = PromptImage.prepareUpload(
                imageId,
                uploaderId,
                "originals/" + uploaderId + "/" + imageId + ".jpg",
                "result.jpg",
                PromptImageContentType.JPEG,
                1_024,
                1_920,
                1_080
        );
        image.completeUpload("\"etag\"", Instant.parse("2026-07-28T01:00:00Z"));
        image.startProcessing();
        image.completeProcessing("watermarked/" + uploaderId + "/" + imageId + ".jpg", 1);
        promptImageRepository.saveAndFlush(PromptImageJpaEntity.from(image));
        return image;
    }
}
