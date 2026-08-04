package com.promsearch.user.application.port.out.profileimage;

import java.net.URI;
import java.time.Instant;

/**
 * 프로필 이미지를 저장소에 직접 업로드할 수 있는 Presigned PUT URL을 발급하는 출력 포트입니다.
 */
public interface PresignProfileImageUploadPort {

    /**
     * Object Key, Content-Type, Content-Length를 서명 조건에 포함한 일회성 PUT URL을 발급합니다.
     *
     * @param objectKey 업로드 대상 Object Key
     * @param contentType 허용할 Content-Type
     * @param contentLength 허용할 정확한 파일 크기(byte)
     * @return 업로드 URL과 만료 시각
     */
    PresignedUpload presignPut(String objectKey, String contentType, long contentLength);

    /**
     * 발급된 Presigned PUT URL 정보입니다.
     *
     * @param uploadUrl 저장소 직접 업로드 URL
     * @param expiresAt URL 만료 시각
     */
    record PresignedUpload(URI uploadUrl, Instant expiresAt) {
    }
}
