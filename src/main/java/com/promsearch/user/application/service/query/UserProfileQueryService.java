package com.promsearch.user.application.service.query;

import com.promsearch.user.application.port.out.user.LoadUserPort;
import com.promsearch.user.application.port.out.profileimage.ProfileImageDeliveryPort;
import com.promsearch.user.application.port.out.user.UserProfileStats;
import com.promsearch.user.application.port.out.user.UserProfileStatsReader;
import com.promsearch.user.application.usecase.GetPublicUserProfileUseCase;
import com.promsearch.user.application.usecase.GetMyProfileUseCase;
import com.promsearch.user.application.usecase.dto.PublicUserProfileInfo;
import com.promsearch.user.application.usecase.dto.UserProfileInfo;
import com.promsearch.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileQueryService implements GetPublicUserProfileUseCase, GetMyProfileUseCase {

    /*
     * 상대 프로필 조회도 사용자 존재 여부와 ACTIVE 상태 검증은 기존 LoadUserPort의 getById 규칙을 재사용합니다.
     * 이렇게 하면 사용자 조회 실패 시 기존 UserErrorCode.USER_NOT_FOUND 흐름을 그대로 따릅니다.
     */
    private final LoadUserPort loadUserPort;

    /*
     * 프로필 상단에 표시할 누적 통계는 user aggregate 자체의 책임이 아니라 prompt 통계를 집계한 값입니다.
     * application 계층은 포트만 알고, 실제 집계 쿼리는 infrastructure adapter에 맡깁니다.
     */
    private final UserProfileStatsReader userProfileStatsReader;
    private final ProfileImageDeliveryPort profileImageDeliveryPort;

    @Override
    public PublicUserProfileInfo getProfile(Long userId) {
        User user = loadUserPort.getById(userId);
        UserProfileStats stats = userProfileStatsReader.getByUserId(userId);

        return PublicUserProfileInfo.from(user, stats, resolveProfileImageUrl(user));
    }

    @Override
    public UserProfileInfo getMyProfile(Long userId) {
        User user = loadUserPort.getById(userId);
        return UserProfileInfo.from(user, resolveProfileImageUrl(user));
    }

    private String resolveProfileImageUrl(User user) {
        return profileImageDeliveryPort.resolve(
                user.getProfileImageUrl(),
                user.getProfileImageObjectKey()
        );
    }
}
