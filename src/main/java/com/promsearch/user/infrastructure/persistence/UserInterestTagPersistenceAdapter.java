package com.promsearch.user.infrastructure.persistence;

import com.promsearch.user.application.port.out.user.SaveUserInterestTagPort;
import com.promsearch.user.application.port.out.user.LoadUserInterestTagPort;
import com.promsearch.user.application.usecase.dto.InterestTagInfo;
import com.promsearch.user.infrastructure.persistence.entity.UserInterestTagJpaEntity;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserInterestTagPersistenceAdapter implements SaveUserInterestTagPort, LoadUserInterestTagPort {

    private final UserInterestTagRepository userInterestTagRepository;

    @Override
    public void save(Long userId, List<Long> tagIds) {
        userInterestTagRepository.saveAll(tagIds.stream()
                .map(tagId -> UserInterestTagJpaEntity.create(userId, tagId))
                .toList());
    }

    @Override
    public void replace(Long userId, List<Long> tagIds) {
        userInterestTagRepository.deleteAllByIdUserId(userId);
        save(userId, tagIds);
    }

    @Override
    public List<InterestTagInfo> listByUserId(Long userId) {
        return userInterestTagRepository.findInterestTagsByUserId(userId)
                .stream()
                .map(tag -> new InterestTagInfo(
                        tag.getId(),
                        tag.getTagName(),
                        com.promsearch.user.domain.enums.InterestTagType.valueOf(tag.getTagType().name())
                ))
                .toList();
    }
}
