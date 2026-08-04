package com.promsearch.prompt.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.promsearch.global.config.JpaConfig;
import com.promsearch.prompt.domain.Prompt;
import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import com.promsearch.prompt.domain.enums.PromptVisibility;
import com.promsearch.prompt.infrastructure.persistence.entity.PostJpaEntity;
import com.promsearch.prompt.infrastructure.persistence.entity.PostStatisticsJpaEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaConfig.class)
class PostStatisticsRepositoryTest {

    @Autowired PostRepository postRepository;
    @Autowired PostStatisticsRepository postStatisticsRepository;

    @Test
    void incrementsReportCountForPublicActivePost() {
        Long postId = savePost(PromptVisibility.PUBLIC);

        int updatedRows = postStatisticsRepository.incrementReportCountIfReportable(postId);

        assertThat(updatedRows).isEqualTo(1);
        assertThat(reportCount(postId)).isEqualTo(1L);
    }

    @Test
    void doesNotIncrementReportCountForPrivatePost() {
        Long postId = savePost(PromptVisibility.PRIVATE);

        int updatedRows = postStatisticsRepository.incrementReportCountIfReportable(postId);

        assertThat(updatedRows).isZero();
        assertThat(reportCount(postId)).isZero();
    }

    private Long savePost(PromptVisibility visibility) {
        Prompt prompt = Prompt.createActive(
                1L,
                "report target",
                "body",
                PromptOutputType.TEXT,
                "description",
                PromptContentType.FREE,
                visibility,
                0L
        );
        PostJpaEntity post = PostJpaEntity.from(prompt);
        post.initializeStatistics(PostStatisticsJpaEntity.create(post));
        return postRepository.saveAndFlush(post).getId();
    }

    private long reportCount(Long postId) {
        return postStatisticsRepository.findById(postId)
                .orElseThrow()
                .toDomain()
                .getReportCount();
    }
}
