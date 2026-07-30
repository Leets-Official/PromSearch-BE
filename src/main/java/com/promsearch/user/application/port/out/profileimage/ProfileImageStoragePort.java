package com.promsearch.user.application.port.out.profileimage;

import java.net.URI;
import java.time.Instant;

/**
 * 프로필 이미지 애플리케이션 계층이 필요한 저장소 기능을 정의하는 출력 포트.
 *
 * <p>공통 Object Storage 기능 중 프로필 이미지 흐름에 필요한 계약만 노출하여
 * 애플리케이션 계층이 S3 SDK나 공통 인프라 구현에 직접 의존하지 않게 한다.</p>
 */
public interface ProfileImageStoragePort {

    /**
     * 클라이언트가 프로필 이미지를 저장소로 직접 업로드할 수 있는 URL을 발급한다.
     *
     * @param objectKey 업로드 대상 객체 키
     * @param contentType 업로드할 이미지 MIME 타입
     * @return 업로드 URL과 만료 시각
     */
    PresignedUpload presignPut(String objectKey, String contentType);

    /**
     * 저장된 프로필 이미지를 조회할 수 있는 서명 URL을 발급한다.
     *
     * @param objectKey 조회 대상 객체 키
     * @return 서명된 조회 URL
     */
    String presignGet(String objectKey);

    /**
     * 업로드 완료 요청의 파일 크기와 MIME 타입을 실제 저장 객체와 대조하기 위해 조회한다.
     *
     * @param objectKey 검증 대상 객체 키
     * @return 검증에 필요한 객체 크기와 MIME 타입
     */
    StoredObjectMetadata getMetadata(String objectKey);

    /**
     * 교체, 삭제 또는 잘못된 업로드로 더 이상 사용하지 않는 프로필 이미지 객체를 삭제한다.
     *
     * @param objectKey 삭제 대상 객체 키
     */
    void delete(String objectKey);

    /**
     * 프로필 이미지 업로드 URL 발급 결과.
     *
     * @param uploadUrl 클라이언트가 PUT 요청을 보낼 URL
     * @param expiresAt URL 만료 시각
     */
    record PresignedUpload(URI uploadUrl, Instant expiresAt) {
    }

    /**
     * 업로드 완료 검증에 사용하는 최소 객체 메타데이터.
     *
     * @param contentLength 객체 크기(byte)
     * @param contentType 객체의 MIME 타입
     */
    record StoredObjectMetadata(long contentLength, String contentType) {
    }
}
