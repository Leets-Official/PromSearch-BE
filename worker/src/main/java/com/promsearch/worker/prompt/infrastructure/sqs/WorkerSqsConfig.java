package com.promsearch.worker.prompt.infrastructure.sqs;

import com.promsearch.prompt.infrastructure.messaging.sqs.WatermarkSqsProperties;
import com.promsearch.common.infrastructure.storage.s3.S3ObjectStorageProperties;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

/** SQS가 활성화된 Worker에서 Long Polling용 클라이언트와 스케줄링 구성 */
@EnableScheduling
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        name = "messaging.sqs.watermark.enabled",
        havingValue = "true"
)
public class WorkerSqsConfig {

    /** 20초 Long Polling보다 긴 HTTP 응답 제한을 가진 SQS 동기 클라이언트 */
    @Bean
    SqsClient workerSqsClient(S3ObjectStorageProperties storageProperties) {
        // TODO: 소비 동시성을 높일 때 Apache HTTP 커넥션 풀로 전환하고
        // maxConnections를 Long Polling 수 + 삭제 요청 동시성 이상으로 설정
        return SqsClient.builder()
                .region(Region.of(storageProperties.region()))
                .httpClientBuilder(UrlConnectionHttpClient.builder()
                        .connectionTimeout(Duration.ofSeconds(2))
                        .socketTimeout(Duration.ofSeconds(25)))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallAttemptTimeout(Duration.ofSeconds(25))
                        .apiCallTimeout(Duration.ofSeconds(30))
                        .build())
                .build();
    }
}
