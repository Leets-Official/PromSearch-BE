package com.promsearch.user.application.usecase.dto;

import com.promsearch.user.domain.User;
import com.promsearch.user.domain.enums.UserGrade;
import com.promsearch.user.domain.enums.UserRole;
import com.promsearch.user.domain.enums.UserStatus;
import java.time.Instant;

public record UserInfo(
        Long userId,
        String email,
        String nickname,
        String name,
        String profileImageUrl,
        Long point,
        UserRole role,
        UserGrade grade,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public static UserInfo from(User user, String profileImageUrl) {
        return new UserInfo(
                user.getUserId().id(),
                user.getEmail(),
                user.getNickname(),
                user.getName(),
                profileImageUrl,
                user.getPoint(),
                user.getRole(),
                user.getGrade(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
