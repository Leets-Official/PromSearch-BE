package com.promsearch.user.application.usecase.dto;

/**
 * 직접 업로드된 프로필 이미지를 사용자에게 연결하기 위한 애플리케이션 명령.
 *
 * @param userId 인증된 사용자 식별자
 * @param objectKey URL 발급 단계에서 서버가 생성한 객체 키
 * @param contentType 클라이언트가 업로드한 이미지 MIME 타입
 * @param fileSize 클라이언트가 업로드한 이미지 크기(byte)
 */
public record CompleteProfileImageUploadCommand(
        Long userId,
        String objectKey,
        String contentType,
        long fileSize
) {

    /**
     * HTTP 요청 값과 인증 사용자 식별자를 하나의 업로드 완료 명령으로 묶는다.
     */
    public static CompleteProfileImageUploadCommand of(
            Long userId,
            String objectKey,
            String contentType,
            long fileSize
    ) {
        return new CompleteProfileImageUploadCommand(userId, objectKey, contentType, fileSize);
    }
}
