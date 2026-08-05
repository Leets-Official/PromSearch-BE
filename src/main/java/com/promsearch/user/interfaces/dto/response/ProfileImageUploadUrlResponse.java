package com.promsearch.user.interfaces.dto.response;

import com.promsearch.user.application.usecase.dto.ProfileImageUploadUrlInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;
import java.time.Instant;

/**
 * 클라이언트가 프로필 이미지를 S3에 직접 업로드할 때 사용하는 응답입니다.
 *
 * @param objectKey 업로드 완료 API에 전달할 Object Key
 * @param uploadUrl S3 Presigned PUT URL
 * @param contentType PUT 요청에 사용할 Content-Type
 * @param contentLength 업로드할 정확한 파일 크기(byte)
 * @param ifNoneMatch 동일 Object Key 덮어쓰기를 막는 조건 값
 * @param expiresAt 업로드 URL 만료 시각
 */
@Schema(description = "프로필 이미지 Presigned PUT URL")
public record ProfileImageUploadUrlResponse(
        String objectKey,
        URI uploadUrl,
        String contentType,
        long contentLength,
        String ifNoneMatch,
        Instant expiresAt
) {

    /**
     * 애플리케이션 결과를 외부 API 응답 모델로 변환한다.
     */
    public static ProfileImageUploadUrlResponse from(ProfileImageUploadUrlInfo info) {
        return new ProfileImageUploadUrlResponse(
                info.objectKey(),
                info.uploadUrl(),
                info.contentType(),
                info.contentLength(),
                "*",
                info.expiresAt()
        );
    }
}
