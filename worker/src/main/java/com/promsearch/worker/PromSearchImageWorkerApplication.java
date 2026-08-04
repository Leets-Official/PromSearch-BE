package com.promsearch.worker;

import com.promsearch.global.config.JpaConfig;
import com.promsearch.prompt.infrastructure.persistence.PromptImagePersistenceAdapter;
import com.promsearch.prompt.infrastructure.persistence.PromptImageRepository;
import com.promsearch.prompt.infrastructure.messaging.sqs.WatermarkSqsProperties;
import com.promsearch.prompt.infrastructure.storage.s3.S3StorageProperties;
import com.promsearch.common.infrastructure.storage.s3.S3ObjectStorageProperties;
import com.promsearch.worker.prompt.infrastructure.image.WatermarkRenderingProperties;
import java.util.Map;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** SQS 작업을 소비하는 HTTP 비활성 이미지 처리 애플리케이션 */
@SpringBootApplication
@EntityScan(basePackages = "com.promsearch")
@EnableJpaRepositories(basePackageClasses = PromptImageRepository.class)
@EnableConfigurationProperties({
        S3StorageProperties.class,
        S3ObjectStorageProperties.class,
        WatermarkSqsProperties.class,
        WatermarkRenderingProperties.class
})
@Import({
        JpaConfig.class,
        PromptImagePersistenceAdapter.class
})
public class PromSearchImageWorkerApplication {

    // TODO: 작업량이 간헐적이고 유휴 Worker 비용이 더 커지는 시점에는 Lambda 전환 비교
    // 버전 메시지 계약과 처리 UseCase는 유지하고 SQS 소비 어댑터만 Handler로 교체
    // 메모리·임시 스토리지·제한 시간·동시 실행 수·cold start·이미지 처리 p95 비용 검증

    /** Worker 전용 설정과 비웹 모드로 별도 JVM 실행 */
    public static void main(String[] args) {
        new SpringApplicationBuilder(PromSearchImageWorkerApplication.class)
                .web(WebApplicationType.NONE)
                .properties(Map.of("spring.config.name", "application-worker"))
                .run(args);
    }
}
