package com.promsearch.prompt.infrastructure.persistence;

import com.promsearch.prompt.application.port.out.prompt.LoadPromptPort;
import com.promsearch.prompt.application.port.out.prompt.PromptInsightTotals;
import com.promsearch.prompt.application.port.out.prompt.PromptPageResult;
import com.promsearch.prompt.domain.Prompt;
import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.infrastructure.persistence.entity.PostJpaEntity;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PromptPersistenceAdapter implements LoadPromptPort {

    private final PostRepository postRepository;

    @Override
    public PromptPageResult listByUserIdAndStatus(Long userId, PromptStatus status, int page, int size) {
        Page<PostJpaEntity> result = postRepository.findByUserIdAndStatusAndDeletedAtIsNullOrderByPublishedAtDesc(
                userId,
                status,
                PageRequest.of(page, size)
        );

        List<Prompt> content = result.getContent().stream()
                .map(PostJpaEntity::toDomain)
                .toList();

        return new PromptPageResult(content, result.getTotalElements());
    }

    @Override
    public PromptInsightTotals sumInsightsByUserId(Long userId) {
        PromptInsightProjection projection = postRepository.sumInsightsByUserId(userId);

        return new PromptInsightTotals(
                projection.getTotalViews(),
                projection.getTotalRecommends(),
                projection.getTotalCopies()
        );
    }
}
