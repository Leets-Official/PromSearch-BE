package com.promsearch.user.application.port.out.profileimage;

/**
 * 업로드된 프로필 이미지 객체의 메타데이터를 조회하는 출력 포트입니다.
 */
public interface LoadProfileImageObjectMetadataPort {

    /**
     * 업로드 완료 검증에 필요한 객체 크기와 Content-Type을 조회합니다.
     *
     * @param objectKey 조회할 프로필 이미지 Object Key
     * @return 저장된 객체 메타데이터
     */
    StoredObjectMetadata getMetadata(String objectKey);

    /**
     * 저장소에 기록된 프로필 이미지 객체 메타데이터입니다.
     *
     * @param contentLength 객체 크기(byte)
     * @param contentType 객체의 Content-Type
     */
    record StoredObjectMetadata(long contentLength, String contentType) {
    }
}
