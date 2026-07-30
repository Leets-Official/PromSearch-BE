package com.promsearch.common.infrastructure.storage;

import java.net.URI;
import java.time.Instant;

/**
 * S3 같은 Object Storage의 공통 기능을 제공하는 기술 추상화.
 *
 * <p>프롬프트 이미지나 프로필 이미지 같은 개별 도메인은 특정 클라우드 SDK에 직접
 * 의존하지 않고 이 인터페이스를 통해 업로드, 조회, 메타데이터 확인, 삭제 기능을 사용한다.</p>
 */
public interface ObjectStorageOperations {

    /**
     * 클라이언트가 지정된 객체를 직접 업로드할 수 있는 서명된 PUT URL을 발급한다.
     *
     * @param objectKey 저장할 객체의 버킷 내 경로
     * @param contentType 업로드 요청에 포함할 MIME 타입
     * @return 업로드 URL과 만료 시각
     */
    PresignedUpload presignPut(String objectKey, String contentType);

    /**
     * 클라이언트가 비공개 객체를 읽을 수 있는 서명된 GET URL을 발급한다.
     *
     * @param objectKey 조회할 객체의 버킷 내 경로
     * @return 만료 시간이 포함된 서명 URL
     */
    String presignGet(String objectKey);

    /**
     * 객체 본문을 내려받지 않고 업로드된 객체의 메타데이터를 조회한다.
     *
     * @param objectKey 확인할 객체의 버킷 내 경로
     * @return 파일 크기, MIME 타입, ETag, 수정 시각
     */
    StoredObjectMetadata getMetadata(String objectKey);

    /**
     * 저장소에서 객체를 삭제한다. 이미 존재하지 않는 객체의 삭제는 성공으로 처리한다.
     *
     * @param objectKey 삭제할 객체의 버킷 내 경로
     */
    void delete(String objectKey);

    /**
     * 서명된 업로드 URL 발급 결과.
     *
     * @param uploadUrl 클라이언트가 PUT 요청을 보낼 URL
     * @param expiresAt URL 만료 시각
     */
    record PresignedUpload(URI uploadUrl, Instant expiresAt) {
    }

    /**
     * 저장된 객체를 검증하기 위한 메타데이터.
     *
     * @param contentLength 객체 크기(byte)
     * @param contentType 객체의 MIME 타입
     * @param etag 저장소가 반환한 객체 식별 태그
     * @param lastModified 마지막 수정 시각
     */
    record StoredObjectMetadata(
            long contentLength,
            String contentType,
            String etag,
            Instant lastModified
    ) {
    }
}
