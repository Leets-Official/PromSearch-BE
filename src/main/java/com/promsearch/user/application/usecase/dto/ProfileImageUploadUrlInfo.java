package com.promsearch.user.application.usecase.dto;

import java.net.URI;
import java.time.Instant;

/**
 * 프로필 이미지 S3 직접 업로드에 필요한 애플리케이션 응답 정보입니다.
 *
 * @param objectKey 업로드 완료 시 다시 전달할 Object Key
 * @param uploadUrl S3 Presigned PUT URL
 * @param contentType 서명에 포함된 Content-Type
 * @param contentLength 서명에 포함된 파일 크기(byte)
 * @param expiresAt Presigned URL 만료 시각
 */
public record ProfileImageUploadUrlInfo(
        String objectKey,
        URI uploadUrl,
        String contentType,
        long contentLength,
        Instant expiresAt
) {
}
