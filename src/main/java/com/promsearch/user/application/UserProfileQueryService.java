package com.promsearch.user.application;

import com.promsearch.user.application.port.out.UserProfileStats;
import com.promsearch.user.application.port.out.UserProfileStatsReader;
import com.promsearch.user.application.port.out.UserRepository;
import com.promsearch.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileQueryService implements GetPublicUserProfileUseCase {

    /*
     * 상대 프로필 조회도 사용자 존재 여부와 ACTIVE 상태 검증은 기존 UserRepository의 getById 규칙을 재사용합니다.
     * 이렇게 하면 사용자 조회 실패 시 기존 UserErrorCode.USER_NOT_FOUND 흐름을 그대로 따릅니다.
     */
    private final UserRepository userRepository;

    /*
     * 프로필 상단에 표시할 누적 통계는 user aggregate 자체의 책임이 아니라 prompt 통계를 집계한 값입니다.
     * application 계층은 포트만 알고, 실제 집계 쿼리는 infrastructure adapter에 맡깁니다.
     */
    private final UserProfileStatsReader userProfileStatsReader;

    @Override
    public PublicUserProfileInfo getProfile(Long userId) {
        /*
         * 1. userRepository.getById로 사용자가 존재하고 활성 상태인지 먼저 확인합니다.
         * 2. 이후 공개 가능한 프롬프트 통계를 별도 reader에서 집계합니다.
         * 3. PublicUserProfileInfo.from에서 이메일/포인트/권한 등 민감 정보는 제외하고 공개 필드만 조립합니다.
         */
        User user = userRepository.getById(userId);
        UserProfileStats stats = userProfileStatsReader.getByUserId(userId);

        return PublicUserProfileInfo.from(user, stats);
    }
}
