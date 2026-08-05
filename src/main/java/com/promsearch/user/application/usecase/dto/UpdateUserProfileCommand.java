package com.promsearch.user.application.usecase.dto;

import com.promsearch.auth.domain.CredentialPolicy;
import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import java.util.List;

/**
 * 사용자 프로필 부분 변경 Command. null 필드는 기존 값을 유지한다.
 *
 * <p>{@code jobTagIds}와 {@code taskTagIds}는 관심 태그를 함께 교체할 때만 사용하며,
 * 둘 다 null이면 기존 관심 태그를 유지하고 둘 중 하나만 null이면 유효하지 않은 요청으로 거절한다.</p>
 */
public record UpdateUserProfileCommand(
        Long userId,
        String nickname,
        String email,
        List<Long> jobTagIds,
        List<Long> taskTagIds
) {

    public UpdateUserProfileCommand {
        // 프로필 서비스가 앞뒤 공백을 제거하므로 실제 저장될 값을 기준으로 정책을 적용한다.
        if (email != null && !email.isBlank()) {
            CredentialPolicy.validateEmail(email.trim());
        }
        if ((jobTagIds == null) != (taskTagIds == null)) {
            throw new UserDomainException(UserErrorCode.INVALID_INTEREST_TAG);
        }
    }

    public static UpdateUserProfileCommand of(
            Long userId,
            String nickname,
            String email,
            List<Long> jobTagIds,
            List<Long> taskTagIds
    ) {
        return new UpdateUserProfileCommand(userId, nickname, email, jobTagIds, taskTagIds);
    }
}
