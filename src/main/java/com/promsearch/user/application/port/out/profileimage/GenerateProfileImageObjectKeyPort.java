package com.promsearch.user.application.port.out.profileimage;

import com.promsearch.user.domain.enums.ProfileImageContentType;

/**
 * 프로필 이미지 객체 키의 생성 규칙과 소유권 검사를 외부로 분리한 출력 포트.
 *
 * <p>애플리케이션 서비스는 실제 경로 접두사나 파일명 형식을 알지 않고 이 포트를 통해
 * 프로필 이미지용 객체 키를 다룬다.</p>
 */
public interface GenerateProfileImageObjectKeyPort {

    /**
     * 사용자와 이미지 타입에 맞는 새로운 프로필 이미지 객체 키를 생성한다.
     *
     * @param userId 이미지 소유자 식별자
     * @param contentType 허용된 프로필 이미지 타입
     * @return 저장소에 사용할 객체 키
     */
    String generate(Long userId, ProfileImageContentType contentType);

    /**
     * 전달받은 객체 키가 해당 사용자의 프로필 이미지 경로와 파일명 규칙을 만족하는지 검사한다.
     *
     * @param objectKey 검사할 객체 키
     * @param userId 요청 사용자 식별자
     * @return 해당 사용자가 소유한 유효한 프로필 이미지 키이면 {@code true}
     */
    boolean isOwnedBy(String objectKey, Long userId);
}
