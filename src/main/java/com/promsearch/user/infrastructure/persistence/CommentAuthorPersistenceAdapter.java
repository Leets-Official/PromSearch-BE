package com.promsearch.user.infrastructure.persistence;

import com.promsearch.community.application.port.out.comment.LoadCommentAuthorPort;
import com.promsearch.community.application.port.out.comment.LoadCommentAuthorPort.CommentAuthorSnapshot;
import com.promsearch.community.domain.exception.CommunityDomainException;
import com.promsearch.community.domain.exception.CommunityErrorCode;
import com.promsearch.user.domain.User;
import com.promsearch.user.domain.enums.UserStatus;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentAuthorPersistenceAdapter implements LoadCommentAuthorPort {

    private final UserRepository userRepository;

    @Override
    public CommentAuthorSnapshot getActiveById(Long userId) {
        if (userId == null || userId <= 0) {
            throw new CommunityDomainException(CommunityErrorCode.INVALID_COMMENT_USER_ID);
        }
        return userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
                .map(UserMapper::toDomain)
                .map(this::toSnapshot)
                .orElseThrow(() -> new CommunityDomainException(CommunityErrorCode.COMMENT_AUTHOR_NOT_FOUND));
    }

    @Override
    public Map<Long, CommentAuthorSnapshot> batchGetByIds(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, CommentAuthorSnapshot> authors = userRepository.findAllById(userIds).stream()
                .map(UserMapper::toDomain)
                .map(this::toSnapshot)
                .collect(Collectors.toMap(CommentAuthorSnapshot::userId, Function.identity()));

        if (authors.size() != userIds.size()) {
            throw new CommunityDomainException(CommunityErrorCode.COMMENT_AUTHOR_NOT_FOUND);
        }
        return authors;
    }

    private CommentAuthorSnapshot toSnapshot(User user) {
        return new CommentAuthorSnapshot(
                user.getUserId().id(),
                user.getNickname(),
                user.getProfileImageUrl()
        );
    }
}
