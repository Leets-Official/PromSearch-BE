package com.promsearch.commerce.infrastructure.persistence;

import com.promsearch.commerce.application.port.out.unlock.CheckPostUnlockPort;
import com.promsearch.commerce.application.port.out.unlock.SavePostUnlockPort;
import com.promsearch.commerce.domain.PostUnlock;
import com.promsearch.commerce.infrastructure.persistence.entity.PostUnlockJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostUnlockPersistenceAdapter implements CheckPostUnlockPort, SavePostUnlockPort {

    private final PostUnlockRepository postUnlockRepository;

    @Override
    public boolean isUnlocked(Long userId, Long postId) {
        return postUnlockRepository.existsByUserIdAndPostId(userId, postId);
    }

    @Override
    public void save(PostUnlock postUnlock) {
        postUnlockRepository.save(PostUnlockJpaEntity.create(
                postUnlock.getPostId(),
                postUnlock.getUserId(),
                postUnlock.getCreatorUserId(),
                postUnlock.getPaidPoint(),
                postUnlock.getCreatorRewardPoint()
        ));
    }
}
