package com.promsearch.worker;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

class PromSearchImageWorkerApplicationTest {

    @DisplayName("이미지 Worker는 HTTP 서버 없이 독립적인 Spring Context로 실행된다")
    @Test
    void startsWithoutWebServer() {
        try (ConfigurableApplicationContext context =
                     new SpringApplicationBuilder(PromSearchImageWorkerApplication.class)
                             .web(WebApplicationType.NONE)
                             .properties(
                                     "spring.config.name=application-worker",
                                     "spring.main.banner-mode=off"
                             )
                             .run(
                                     "--spring.datasource.url=jdbc:h2:mem:worker_context;MODE=PostgreSQL",
                                     "--spring.jpa.hibernate.ddl-auto=create-drop",
                                     "--storage.s3.bucket=promsearch-worker-test",
                                     "--storage.s3.region=ap-northeast-2"
                             )) {
            assertThat(context).isInstanceOf(AnnotationConfigApplicationContext.class);
            assertThat(context.getEnvironment().getProperty("spring.application.name"))
                    .isEqualTo("promsearch-image-worker");
        }
    }
}
