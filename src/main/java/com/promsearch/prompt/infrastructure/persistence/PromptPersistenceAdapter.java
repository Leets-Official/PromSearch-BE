package com.promsearch.prompt.infrastructure.persistence;

import com.promsearch.prompt.application.port.out.prompt.SavePromptPort;
import com.promsearch.prompt.domain.Prompt;
import com.promsearch.prompt.domain.Tag;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import com.promsearch.prompt.infrastructure.persistence.entity.PostJpaEntity;
import com.promsearch.prompt.infrastructure.persistence.entity.PostStatisticsJpaEntity;
import com.promsearch.prompt.infrastructure.persistence.entity.PostTagJpaEntity;
import com.promsearch.prompt.infrastructure.persistence.entity.TagJpaEntity;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PromptPersistenceAdapter implements SavePromptPort {

    private final PostRepository postRepository;
    private final TagRepository tagRepository;

    @Override
    public Prompt create(Prompt prompt, List<Tag> tags) {
        PostJpaEntity post = postRepository.saveAndFlush(PostJpaEntity.from(prompt));

        List<Long> tagIds = tags.stream()
                .map(tag -> tag.getTagId().id())
                .toList();
        List<TagJpaEntity> tagEntities = tagRepository.findAllById(tagIds);
        if (tagEntities.size() != tagIds.size()) {
            throw new PromptDomainException(PromptErrorCode.TAG_NOT_FOUND);
        }

        for (TagJpaEntity tag : tagEntities) {
            post.addPostTag(PostTagJpaEntity.create(post, tag));
        }
        post.initializeStatistics(PostStatisticsJpaEntity.create(post));
        postRepository.flush();
        return post.toDomain();
    }
}
