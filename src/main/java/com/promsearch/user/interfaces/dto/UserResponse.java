package com.promsearch.user.interfaces.dto;

import com.promsearch.user.application.UserInfo;
import com.promsearch.user.domain.enums.UserGrade;
import com.promsearch.user.domain.enums.UserRole;
import com.promsearch.user.domain.enums.UserStatus;
import java.time.Instant;

public record UserResponse(
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

    public static UserResponse from(UserInfo userInfo) {
        return new UserResponse(
                userInfo.userId(),
                userInfo.email(),
                userInfo.nickname(),
                userInfo.name(),
                userInfo.profileImageUrl(),
                userInfo.point(),
                userInfo.role(),
                userInfo.grade(),
                userInfo.status(),
                userInfo.createdAt(),
                userInfo.updatedAt()
        );
    }
}
