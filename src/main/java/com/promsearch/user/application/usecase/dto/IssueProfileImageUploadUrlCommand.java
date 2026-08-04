package com.promsearch.user.application.usecase.dto;

/**
 * 프로필 이미지 직접 업로드 URL 발급 명령입니다.
 *
 * @param userId 업로드할 사용자 ID
 * @param contentType 업로드 이미지 MIME 타입
 * @param fileSize 업로드 파일 크기(byte)
 */
public record IssueProfileImageUploadUrlCommand(
        Long userId,
        String contentType,
        long fileSize
) {
}
