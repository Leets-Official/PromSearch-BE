package com.promsearch.prompt.infrastructure.storage.s3;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/** AWS 기본 자격 증명 체인 기반 S3 클라이언트 구성 */
@Configuration(proxyBeanMethods = false)
public class S3StorageConfig {

    /** HeadObject·DeleteObject 호출용 동기 S3 클라이언트 */
    @Bean
    public S3Client s3Client(S3StorageProperties properties) {
        // TODO: 부하 테스트에서 병목 확인 시 Apache HTTP 커넥션 풀과 연결·응답 타임아웃 설정 추가
        /*
         * 전환 형태 예시:
         * build.gradle:
         * implementation 'software.amazon.awssdk:apache-client'
         *
         * .httpClientBuilder(ApacheHttpClient.builder()
         *         .maxConnections(50)
         *         .connectionTimeout(Duration.ofSeconds(2))
         *         .socketTimeout(Duration.ofSeconds(5)))
         */
        return S3Client.builder()
                .region(Region.of(properties.region()))
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build();
    }

    /** Presigned PUT URL 발급용 S3 Presigner */
    @Bean
    public S3Presigner s3Presigner(S3StorageProperties properties) {
        return S3Presigner.builder()
                .region(Region.of(properties.region()))
                .build();
    }
}
