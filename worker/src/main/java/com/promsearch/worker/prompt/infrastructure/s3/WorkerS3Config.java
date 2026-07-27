package com.promsearch.worker.prompt.infrastructure.s3;

import com.promsearch.prompt.infrastructure.storage.s3.S3StorageProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/** Worker가 원본 다운로드와 결과 업로드에 사용하는 S3 클라이언트 구성 */
@Configuration(proxyBeanMethods = false)
public class WorkerS3Config {

    /** AWS 기본 자격 증명 체인을 사용하는 동기 S3 클라이언트 */
    @Bean
    S3Client workerS3Client(S3StorageProperties properties) {
        // TODO: 동시 처리량을 높일 때 Apache HTTP 커넥션 풀과 작업 시간 기반 타임아웃 적용
        return S3Client.builder()
                .region(Region.of(properties.region()))
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build();
    }
}
