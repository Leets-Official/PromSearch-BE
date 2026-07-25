package com.promsearch.user.application;

import com.promsearch.user.application.port.out.UserProfileStats;
import com.promsearch.user.domain.User;
import com.promsearch.user.domain.enums.UserGrade;
import java.time.Instant;

public record PublicUserProfileInfo(
        Long userId,
        String nickname,
        String name,
        String profileImageUrl,
        UserGrade grade,
        long promptCount,
        long totalLikeCount,
        long totalViewCount,
        Instant createdAt
) {

    public static PublicUserProfileInfo from(User user, UserProfileStats stats) {
        return new PublicUserProfileInfo(
                user.getUserId().id(),
                user.getNickname(),
                user.getName(),
                user.getProfileImageUrl(),
                user.getGrade(),
                stats.promptCount(),
                stats.totalLikeCount(),
                stats.totalViewCount(),
                user.getCreatedAt()
        );
    }
}
