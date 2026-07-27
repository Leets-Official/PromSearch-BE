package com.promsearch.prompt.infrastructure.messaging.sqs;

import com.promsearch.prompt.application.port.out.promptimage.ClaimPromptImageWatermarkJobsPort;
import com.promsearch.prompt.application.port.out.promptimage.PublishPromptImageWatermarkJobPort;
import com.promsearch.prompt.application.port.out.promptimage.UpdatePromptImageWatermarkOutboxPort;
import com.promsearch.prompt.application.service.command.PromptImageWatermarkOutboxPublisher;
import com.promsearch.prompt.application.service.command.PromptImageWatermarkOutboxPublisher.PublicationPolicy;
import com.promsearch.prompt.application.usecase.PublishPendingPromptImageWatermarkJobsUseCase;
import com.promsearch.prompt.infrastructure.storage.s3.S3StorageProperties;
import java.time.Clock;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

/** SQS가 활성화된 API에서 Outbox 발행기와 스케줄러를 조립 */
@EnableScheduling
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        name = "messaging.sqs.watermark.enabled",
        havingValue = "true"
)
public class PromptImageWatermarkSqsPublicationConfig {

    /** 기본 AWS 자격 증명 체인과 서울 리전을 사용하는 SQS 동기 클라이언트 */
    @Bean
    SqsClient watermarkPublisherSqsClient(S3StorageProperties storageProperties) {
        // TODO: 발행 동시성을 높일 때 Apache HTTP 커넥션 풀로 전환하고
        // 인스턴스별 maxConnections를 Outbox 전송 동시성 이상으로 설정한 뒤 timeout·재시도 지표 검증
        // 다중 인스턴스의 작업 선점은 HTTP 풀이 아닌 DB 잠금·선점 임대로 조정
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

    /** AWS SDK 전송 구현을 Outbox 발행 출력 포트에 연결 */
    @Bean
    PublishPromptImageWatermarkJobPort publishPromptImageWatermarkJobPort(
            SqsClient watermarkPublisherSqsClient,
            WatermarkSqsProperties properties
    ) {
        return new SqsPromptImageWatermarkJobPublisher(
                watermarkPublisherSqsClient,
                properties
        );
    }

    /** 선점·SQS 전송·발행 결과 저장 포트를 큐 비종속 발행 서비스에 연결 */
    @Bean
    PublishPendingPromptImageWatermarkJobsUseCase publishPendingWatermarkJobsUseCase(
            ClaimPromptImageWatermarkJobsPort claimJobsPort,
            PublishPromptImageWatermarkJobPort publishJobPort,
            UpdatePromptImageWatermarkOutboxPort updateOutboxPort,
            WatermarkSqsProperties properties
    ) {
        return new PromptImageWatermarkOutboxPublisher(
                claimJobsPort,
                publishJobPort,
                updateOutboxPort,
                new PublicationPolicy(
                        properties.publisherBatchSize(),
                        properties.claimLease(),
                        properties.initialRetryDelay(),
                        properties.maximumRetryDelay()
                ),
                Clock.systemUTC()
        );
    }

    /** API 프로세스에서 Outbox 폴링을 시작하는 스케줄러 등록 */
    @Bean
    PromptImageWatermarkOutboxScheduler promptImageWatermarkOutboxScheduler(
            PublishPendingPromptImageWatermarkJobsUseCase useCase
    ) {
        return new PromptImageWatermarkOutboxScheduler(useCase);
    }
}
