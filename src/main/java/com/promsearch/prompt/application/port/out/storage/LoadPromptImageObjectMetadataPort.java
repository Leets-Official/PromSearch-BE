package com.promsearch.prompt.application.port.out.storage;

import java.time.Instant;

/** 이미지 객체 메타데이터 조회 포트 */
public interface LoadPromptImageObjectMetadataPort {

    /** 객체 크기·Content-Type·ETag·수정 시각 반환 */
    StoredObjectMetadata getMetadata(String objectKey);

    record StoredObjectMetadata(
            long contentLength,
            String contentType,
            String etag,
            Instant lastModified
    ) {
    }
}
