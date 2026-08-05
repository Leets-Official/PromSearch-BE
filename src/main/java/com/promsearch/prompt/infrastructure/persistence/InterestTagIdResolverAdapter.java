package com.promsearch.prompt.infrastructure.persistence;

import com.promsearch.prompt.domain.enums.TagType;
import com.promsearch.user.application.port.out.tag.InterestTagRow;
import com.promsearch.user.application.port.out.tag.LoadUserInterestTagsPort;
import com.promsearch.user.application.port.out.tag.ResolveInterestTagIdsPort;
import com.promsearch.user.domain.enums.InterestTagType;
import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InterestTagIdResolverAdapter implements ResolveInterestTagIdsPort, LoadUserInterestTagsPort {

    private final InterestTagLookupRepository tagRepository;
    private final UserInterestTagLookupRepository userInterestTagLookupRepository;

    @Override
    public List<Long> resolve(InterestTagType type, List<Long> tagIds) {
        if (tagIds.isEmpty()) {
            return List.of();
        }

        List<Long> resolvedTagIds = tagRepository.findIdsByTypeAndIds(TagType.valueOf(type.name()), tagIds);
        if (resolvedTagIds.size() != tagIds.size()) {
            throw new UserDomainException(UserErrorCode.INVALID_INTEREST_TAG);
        }
        return resolvedTagIds;
    }

    @Override
    public List<InterestTagRow> loadByUserId(Long userId) {
        return userInterestTagLookupRepository.findTagsByUserId(userId).stream()
                .map(row -> new InterestTagRow(
                        row.getTagId(),
                        InterestTagType.valueOf(row.getType().name()),
                        row.getTagName()
                ))
                .toList();
    }
}
