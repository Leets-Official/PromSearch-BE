package com.promsearch.prompt.infrastructure.persistence;

import com.promsearch.community.application.port.out.prompt.EnsurePromptInteractionTargetPort;
import com.promsearch.community.domain.exception.CommunityDomainException;
import com.promsearch.community.domain.exception.CommunityErrorCode;
import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.domain.enums.PromptVisibility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PromptInteractionTargetPersistenceAdapter implements EnsurePromptInteractionTargetPort {

    private final PostRepository postRepository;

    @Override
    public void ensureActivePublicPrompt(Long promptId) {
        boolean exists = postRepository.existsByIdAndStatusAndVisibilityAndDeletedAtIsNull(
                promptId,
                PromptStatus.ACTIVE,
                PromptVisibility.PUBLIC
        );
        if (!exists) {
            throw new CommunityDomainException(CommunityErrorCode.INTERACTION_TARGET_NOT_FOUND);
        }
    }
}
