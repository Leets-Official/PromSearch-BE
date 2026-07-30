package com.promsearch.user.application.usecase;

import com.promsearch.user.application.usecase.dto.UserProfileInfo;

/**
 * 인증된 사용자의 프로필 정보를 조회하는 입력 포트.
 */
public interface GetMyProfileUseCase {

    /**
     * 사용자 정보와 현재 저장 방식에 맞게 해석된 프로필 이미지 URL을 조회한다.
     *
     * @param userId 인증된 사용자 식별자
     * @return 내 프로필 화면에 필요한 사용자 정보
     */
    UserProfileInfo getMyProfile(Long userId);
}
