package com.promsearch.user.application.port.out.social;

/**
 * 탈퇴한 사용자의 외부 로그인 연결을 제거하는 출력 포트.
 *
 * <p>소셜 제공자 측 연결은 유지하되, 우리 서비스의 연결 정보만 제거하여
 * 동일한 소셜 계정으로 다시 가입할 수 있게 한다.</p>
 */
public interface DeleteUserSocialAccountsPort {

    /** 사용자의 모든 내부 소셜 로그인 연결을 제거한다. */
    void deleteByUserId(Long userId);
}
