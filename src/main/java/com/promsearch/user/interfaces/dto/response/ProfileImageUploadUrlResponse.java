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
 * @param contentLength 서명된 업로드 파일 크기(byte)
 * @param ifNoneMatch Object Key 덮어쓰기를 방지하는 조건 값
 * @param expiresAt Presigned URL 만료 시각
 */
@Schema(description = "프로필 이미지 S3 직접 업로드 정보")
public record ProfileImageUploadUrlResponse(
        @Schema(description = "업로드 완료 API에 전달할 Object Key")
        String objectKey,

        @Schema(description = "S3 업로드용 Presigned PUT URL")
        URI uploadUrl,

        @Schema(description = "PUT 요청에 반드시 포함할 Content-Type 헤더 값", example = "image/jpeg")
        String contentType,

        @Schema(description = "서명에 포함된 업로드 파일 크기(byte)", example = "1024")
        long contentLength,

        @Schema(description = "동일 Object Key 덮어쓰기를 막기 위해 PUT 요청에 포함할 If-None-Match 값", example = "*")
        String ifNoneMatch,

        @Schema(description = "업로드 URL 만료 시각")
        Instant expiresAt
) {

    /**
     * @param info 애플리케이션 계층의 업로드 URL 정보
     * @return HTTP 업로드 URL 응답
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
