package com.promsearch.user.application.usecase.dto;

/**
 * 프로필 이미지 업로드 URL 발급에 필요한 애플리케이션 명령.
 *
 * @param userId 인증된 사용자 식별자
 * @param contentType 업로드할 이미지 MIME 타입
 * @param fileSize 업로드할 이미지 크기(byte)
 */
public record IssueProfileImageUploadUrlCommand(
        Long userId,
        String contentType,
        long fileSize
) {

    /**
     * HTTP 요청 값과 인증 사용자 식별자를 하나의 애플리케이션 명령으로 묶는다.
     */
    public static IssueProfileImageUploadUrlCommand of(Long userId, String contentType, long fileSize) {
        return new IssueProfileImageUploadUrlCommand(userId, contentType, fileSize);
    }
}
