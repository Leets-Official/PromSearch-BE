package com.promsearch.worker;

import com.promsearch.global.config.JpaConfig;
import com.promsearch.prompt.infrastructure.persistence.PromptImagePersistenceAdapter;
import com.promsearch.prompt.infrastructure.persistence.PromptImageRepository;
import com.promsearch.prompt.infrastructure.messaging.sqs.WatermarkSqsProperties;
import com.promsearch.prompt.infrastructure.storage.s3.S3StorageProperties;
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
        WatermarkSqsProperties.class,
        WatermarkRenderingProperties.class
})
@Import({
        JpaConfig.class,
        PromptImagePersistenceAdapter.class
})
public class PromSearchImageWorkerApplication {

    /** Worker 전용 설정과 비웹 모드로 별도 JVM 실행 */
    public static void main(String[] args) {
        new SpringApplicationBuilder(PromSearchImageWorkerApplication.class)
                .web(WebApplicationType.NONE)
                .properties(Map.of("spring.config.name", "application-worker"))
                .run(args);
    }
}
