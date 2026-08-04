package com.promsearch.common.infrastructure.storage.s3;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * 공통 Object Storage 어댑터가 사용하는 AWS S3 클라이언트 구성.
 */
@Configuration(proxyBeanMethods = false)
public class S3ObjectStorageConfig {

    /**
     * 객체 메타데이터 조회와 삭제처럼 실제 S3 API를 호출하는 동기 클라이언트를 생성한다.
     */
    @Bean
    public S3Client s3Client(S3ObjectStorageProperties properties) {
        return S3Client.builder()
                .region(Region.of(properties.region()))
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build();
    }

    /**
     * 자격 증명을 노출하지 않고 제한 시간 동안 PUT/GET을 허용하는 URL 서명기를 생성한다.
     */
    @Bean
    public S3Presigner s3Presigner(S3ObjectStorageProperties properties) {
        return S3Presigner.builder()
                .region(Region.of(properties.region()))
                .build();
    }
}
