package com.promsearch.user.application.usecase.dto;

import com.promsearch.user.application.port.out.user.UserProfileStats;
import com.promsearch.user.domain.User;
import com.promsearch.user.domain.enums.UserGrade;
import java.time.Instant;

/*
 * 상대 프로필 화면에 노출할 수 있는 공개 정보만 담는 application result입니다.
 * User 도메인 객체에는 이메일, 이름, 비밀번호, 포인트, 권한 같은 민감하거나 본인 전용인 값도 있으므로
 * Controller로 바로 넘기지 않고 이 Info 객체에서 공개 필드만 선별합니다.
 */
public record PublicUserProfileInfo(
        Long userId,
        String nickname,
        String profileImageUrl,
        UserGrade grade,
        long promptCount,
        long totalLikeCount,
        long totalViewCount,
        Instant createdAt
) {

    public static PublicUserProfileInfo from(User user, UserProfileStats stats) {
        /*
         * 공개 프로필 응답에서는 user.getEmail(), user.getName(), user.getPoint(), user.getRole() 등을 절대 포함하지 않습니다.
         * 카드에서 작성자 프로필을 열 때 필요한 식별/표시 정보와 공개 통계만 내려줍니다.
         */
        return new PublicUserProfileInfo(
                user.getUserId().id(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getGrade(),
                stats.promptCount(),
                stats.totalLikeCount(),
                stats.totalViewCount(),
                user.getCreatedAt()
        );
    }
}
