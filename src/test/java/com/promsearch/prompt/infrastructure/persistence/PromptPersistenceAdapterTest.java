package com.promsearch.prompt.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.promsearch.global.config.JpaConfig;
import com.promsearch.prompt.domain.Prompt;
import com.promsearch.prompt.domain.Tag;
import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.domain.enums.PromptVisibility;
import com.promsearch.prompt.domain.enums.TagType;
import com.promsearch.prompt.infrastructure.persistence.entity.PostJpaEntity;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@Import({
        JpaConfig.class,
        PromptPersistenceAdapter.class,
        TagPersistenceAdapter.class
})
class PromptPersistenceAdapterTest {

    @Autowired
    private PromptPersistenceAdapter promptPersistenceAdapter;

    @Autowired
    private TagPersistenceAdapter tagPersistenceAdapter;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("ACTIVE 프롬프트와 공개 범위, 태그, 0으로 초기화된 통계를 함께 저장한다")
    @Test
    void savePromptAggregate() {
        Tag job = tagPersistenceAdapter.create(Tag.create(
                TagType.JOB,
                "개발",
                "개발",
                false
        ));
        Prompt prompt = Prompt.createActive(
                1L,
                "프롬프트 제목",
                "프롬프트 본문",
                PromptOutputType.TEXT,
                "설명",
                PromptContentType.FREE,
                PromptVisibility.PRIVATE,
                0L
        );

        Prompt saved = promptPersistenceAdapter.create(prompt, List.of(job));
        entityManager.clear();
        PostJpaEntity entity = postRepository.findById(saved.getPromptId().id()).orElseThrow();

        assertThat(entity.getStatus()).isEqualTo(PromptStatus.ACTIVE);
        assertThat(entity.getVisibility()).isEqualTo(PromptVisibility.PRIVATE);
        assertThat(entity.getPostTags()).hasSize(1);
        assertThat(entity.getStatistics()).isNotNull();
        assertThat(entity.getStatistics().toDomain().getViewCount()).isZero();
        assertThat(entity.getStatistics().toDomain().getCopyCount()).isZero();
        assertThat(entity.getStatistics().toDomain().getLikeCount()).isZero();
        assertThat(entity.getThumbnailImageUrl()).isNull();
    }

    @DisplayName("AI 모델 태그의 정규화 이름은 타입 안에서 중복될 수 없다")
    @Test
    void customAiModelNormalizedNameIsUnique() {
        tagRepository.saveAndFlush(com.promsearch.prompt.infrastructure.persistence.entity.TagJpaEntity.create(
                TagType.AI_MODEL,
                "GPT 4.1 Mini",
                "gpt4.1mini",
                true
        ));

        assertThatThrownBy(() -> tagRepository.saveAndFlush(
                com.promsearch.prompt.infrastructure.persistence.entity.TagJpaEntity.create(
                        TagType.AI_MODEL,
                        "gpt4.1mini",
                        "gpt4.1mini",
                        true
                )
        )).isInstanceOf(DataIntegrityViolationException.class);
    }
}
