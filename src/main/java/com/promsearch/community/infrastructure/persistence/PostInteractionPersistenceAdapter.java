package com.promsearch.community.infrastructure.persistence;

import com.promsearch.community.application.port.out.postinteraction.CreatePostInteractionPort;
import com.promsearch.community.application.port.out.postinteraction.DeletePostInteractionPort;
import com.promsearch.community.application.port.out.postinteraction.DeletePostInteractionIfPresentPort;
import com.promsearch.community.domain.PostInteraction;
import com.promsearch.community.domain.enums.InteractionType;
import com.promsearch.community.domain.exception.CommunityDomainException;
import com.promsearch.community.domain.exception.CommunityErrorCode;
import com.promsearch.community.infrastructure.persistence.entity.PostInteractionJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostInteractionPersistenceAdapter implements
        CreatePostInteractionPort,
        DeletePostInteractionPort,
        DeletePostInteractionIfPresentPort {

    private final PostInteractionRepository postInteractionRepository;

    @Override
    public PostInteraction create(PostInteraction postInteraction) {
        try {
            return postInteractionRepository.saveAndFlush(PostInteractionJpaEntity.from(postInteraction))
                    .toDomain();
        } catch (DataIntegrityViolationException exception) {
            throw new CommunityDomainException(CommunityErrorCode.ALREADY_INTERACTED);
        }
    }

    @Override
    public void delete(Long userId, Long postId, InteractionType interactionType) {
        int deletedCount = postInteractionRepository.deleteByUserIdAndPostIdAndInteractionType(
                userId,
                postId,
                interactionType
        );
        if (deletedCount == 0) {
            throw new CommunityDomainException(CommunityErrorCode.INTERACTION_NOT_FOUND);
        }
    }

    @Override
    public void deleteIfPresent(Long userId, Long postId, InteractionType interactionType) {
        postInteractionRepository.deleteByUserIdAndPostIdAndInteractionType(
                userId,
                postId,
                interactionType
        );
    }
}
