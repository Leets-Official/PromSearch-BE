package com.promsearch.commerce.infrastructure.persistence;

import com.promsearch.commerce.application.port.out.unlock.CheckPostUnlockPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostUnlockPersistenceAdapter implements CheckPostUnlockPort {

    private final PostUnlockRepository postUnlockRepository;

    @Override
    public boolean isUnlocked(Long userId, Long postId) {
        return postUnlockRepository.existsByUserIdAndPostId(userId, postId);
    }
}
