package com.promsearch.worker.prompt.infrastructure.sqs;

import com.promsearch.prompt.infrastructure.messaging.sqs.WatermarkSqsProperties;
import com.promsearch.common.infrastructure.storage.s3.S3ObjectStorageProperties;
import com.promsearch.worker.prompt.infrastructure.image.WatermarkRenderingProperties;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    @Bean(destroyMethod = "shutdown")
    ExecutorService watermarkTaskExecutor(WatermarkRenderingProperties properties) {
        return Executors.newFixedThreadPool(
                properties.concurrency(),
                Thread.ofPlatform().name("watermark-worker-", 0).factory()
        );
    }

    /** 20초 Long Polling보다 긴 HTTP 응답 제한을 가진 SQS 동기 클라이언트 */
    @Bean
    SqsClient workerSqsClient(S3ObjectStorageProperties storageProperties) {
        // 동시성을 올릴 때는 SQS 연결 사용량과 삭제 요청 지연을 부하 테스트로 함께 확인한다.
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
