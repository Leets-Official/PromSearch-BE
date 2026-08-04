package com.promsearch.user.interfaces.dto.response;

import com.promsearch.user.application.usecase.dto.UserInfo;
import com.promsearch.user.domain.enums.UserGrade;
import com.promsearch.user.domain.enums.UserRole;
import com.promsearch.user.domain.enums.UserStatus;
import java.time.Instant;

public record UserResponse(
        Long userId,
        String email,
        String nickname,
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
