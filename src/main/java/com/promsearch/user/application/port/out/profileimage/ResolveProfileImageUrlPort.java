package com.promsearch.user.application.port.out.profileimage;

/**
 * 저장된 프로필 이미지 Object Key를 외부 공개 조회 URL로 변환하는 출력 포트입니다.
 */
public interface ResolveProfileImageUrlPort {

    /**
     * @param objectKey 공개할 프로필 이미지 Object Key
     * @return CloudFront, CDN 또는 S3 기반의 공개 조회 URL
     */
    String resolve(String objectKey);
}
