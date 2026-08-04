package com.promsearch.user.application.usecase;

/**
 * 사용자의 현재 프로필 이미지를 제거하는 유스케이스입니다.
 */
public interface DeleteProfileImageUseCase {

    /**
     * 프로필 이미지 연결을 해제하고 자사 저장소 객체가 있으면 커밋 후 삭제합니다.
     * 이미 프로필 이미지가 없는 경우에도 정상 종료합니다.
     *
     * @param userId 대상 사용자 ID
     */
    void delete(Long userId);
}
