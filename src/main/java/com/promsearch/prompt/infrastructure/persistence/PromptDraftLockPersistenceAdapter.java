package com.promsearch.prompt.infrastructure.persistence;

import com.promsearch.prompt.application.port.out.prompt.LockPromptDraftPort;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PromptDraftLockPersistenceAdapter implements LockPromptDraftPort {

    private final EntityManager entityManager;

    @Override
    public void lockByUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new PromptDomainException(PromptErrorCode.INVALID_PROMPT_USER_ID);
        }
        entityManager.createNativeQuery(
                        "select pg_advisory_xact_lock(hashtextextended(cast(:lockKey as text), 0))"
                )
                .setParameter("lockKey", "PROMPT_DRAFT:" + userId)
                .getSingleResult();
    }
}
