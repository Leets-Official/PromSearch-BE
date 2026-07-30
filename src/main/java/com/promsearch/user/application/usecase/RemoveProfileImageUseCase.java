package com.promsearch.user.application.usecase;

/**
 * 사용자의 현재 프로필 이미지를 제거하는 입력 포트.
 */
public interface RemoveProfileImageUseCase {

    /**
     * DB의 외부 URL과 객체 키 연결을 해제하고 기존 저장 객체 삭제를 예약한다.
     *
     * @param userId 인증된 사용자 식별자
     */
    void remove(Long userId);
}
