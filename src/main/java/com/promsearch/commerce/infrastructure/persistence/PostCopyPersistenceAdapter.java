package com.promsearch.commerce.infrastructure.persistence;

import com.promsearch.commerce.application.port.out.copy.CheckPostCopyPort;
import com.promsearch.commerce.application.port.out.copy.SavePostCopyPort;
import com.promsearch.commerce.domain.PostCopy;
import com.promsearch.commerce.infrastructure.persistence.entity.PostCopyJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostCopyPersistenceAdapter implements CheckPostCopyPort, SavePostCopyPort {

    private final PostCopyRepository postCopyRepository;

    @Override
    public boolean isCopied(Long userId, Long postId) {
        return postCopyRepository.existsByUserIdAndPostId(userId, postId);
    }

    @Override
    public void save(PostCopy postCopy) {
        postCopyRepository.save(PostCopyJpaEntity.from(postCopy));
    }
}
