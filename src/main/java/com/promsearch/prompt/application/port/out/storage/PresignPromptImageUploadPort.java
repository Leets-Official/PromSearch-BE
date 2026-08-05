package com.promsearch.prompt.application.port.out.storage;

import java.net.URI;
import java.time.Instant;

/** 이미지 업로드 URL 서명 포트 */
public interface PresignPromptImageUploadPort {

    /** Object Key·Content-Type·파일 크기 기반 PUT URL 및 만료 시각 반환 */
    PresignedUpload presignPut(String objectKey, String contentType, long contentLength);

    record PresignedUpload(
            URI uploadUrl,
            Instant expiresAt
    ) {
    }
}
