package com.promsearch.prompt.infrastructure.persistence;

import com.promsearch.prompt.domain.enums.TagType;
import com.promsearch.user.application.port.out.tag.ResolveInterestTagIdsPort;
import com.promsearch.user.domain.enums.InterestTagType;
import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InterestTagIdResolverAdapter implements ResolveInterestTagIdsPort {

    private final InterestTagLookupRepository tagRepository;

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
}
