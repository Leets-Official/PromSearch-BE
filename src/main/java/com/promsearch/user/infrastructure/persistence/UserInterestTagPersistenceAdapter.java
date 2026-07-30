package com.promsearch.user.infrastructure.persistence;

import com.promsearch.prompt.domain.enums.TagType;
import com.promsearch.prompt.infrastructure.persistence.entity.TagJpaEntity;
import com.promsearch.user.application.port.out.user.SaveUserInterestTagPort;
import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import com.promsearch.user.infrastructure.persistence.entity.UserInterestTagJpaEntity;
import com.promsearch.user.infrastructure.persistence.entity.UserJpaEntity;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserInterestTagPersistenceAdapter implements SaveUserInterestTagPort {

    private final InterestTagCatalogRepository tagRepository;
    private final UserRepository userRepository;
    private final UserInterestTagRepository userInterestTagRepository;

    @Override
    public void save(Long userId, List<String> jobTags, List<String> taskTags) {
        UserJpaEntity user = userRepository.getReferenceById(userId);
        List<TagJpaEntity> tags = new ArrayList<>();
        tags.addAll(loadAll(TagType.JOB, jobTags));
        tags.addAll(loadAll(TagType.TASK, taskTags));

        userInterestTagRepository.saveAll(tags.stream()
                .map(tag -> UserInterestTagJpaEntity.create(user, tag))
                .toList());
    }

    private List<TagJpaEntity> loadAll(TagType type, List<String> names) {
        if (names.isEmpty()) {
            return List.of();
        }

        List<TagJpaEntity> tags = tagRepository.findAllByTagTypeAndTagNameIn(type, names);
        if (tags.size() != names.size()) {
            throw new UserDomainException(UserErrorCode.INVALID_INTEREST_TAG);
        }
        return tags;
    }
}
