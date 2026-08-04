package com.promsearch.user.application.port.out.profileimage;

import com.promsearch.user.domain.enums.ProfileImageContentType;
import java.util.UUID;

/**
 * 사용자별 프로필 이미지 Object Key를 생성하고 소유 경로를 검증하는 출력 포트입니다.
 */
public interface GenerateProfileImageObjectKeyPort {

    /**
     * 프로필 이미지 저장에 사용할 Object Key를 생성합니다.
     *
     * @param userId 이미지 소유 사용자 ID
     * @param imageId 파일명에 사용할 고유 이미지 ID
     * @param contentType 이미지 형식
     * @return {@code profiles/{userId}/{imageId}.{extension}} 형식의 Object Key
     */
    String generate(Long userId, UUID imageId, ProfileImageContentType contentType);

    /**
     * Object Key가 지정 사용자의 프로필 이미지 경로와 파일명 규칙을 만족하는지 확인합니다.
     *
     * @param userId 소유권을 확인할 사용자 ID
     * @param objectKey 확인할 Object Key
     * @return 사용자 소유 경로이면 {@code true}
     */
    boolean isOwnedBy(Long userId, String objectKey);
}
