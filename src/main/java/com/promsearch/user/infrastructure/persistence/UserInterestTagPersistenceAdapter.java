package com.promsearch.user.infrastructure.persistence;

import com.promsearch.user.application.port.out.user.SaveUserInterestTagPort;
import com.promsearch.user.infrastructure.persistence.entity.UserInterestTagJpaEntity;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserInterestTagPersistenceAdapter implements SaveUserInterestTagPort {

    private final UserInterestTagRepository userInterestTagRepository;

    @Override
    public void save(Long userId, List<Long> tagIds) {
        userInterestTagRepository.saveAll(tagIds.stream()
                .map(tagId -> UserInterestTagJpaEntity.create(userId, tagId))
                .toList());
    }

    @Override
    public void replaceAll(Long userId, List<Long> tagIds) {
        userInterestTagRepository.deleteByIdUserId(userId);
        save(userId, tagIds);
    }
}
