package com.promsearch.worker;

import java.util.Map;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/** SQS 작업을 소비하는 HTTP 비활성 이미지 처리 애플리케이션 */
@SpringBootApplication
public class PromSearchImageWorkerApplication {

    /** Worker 전용 설정과 비웹 모드로 별도 JVM 실행 */
    public static void main(String[] args) {
        new SpringApplicationBuilder(PromSearchImageWorkerApplication.class)
                .web(WebApplicationType.NONE)
                .properties(Map.of("spring.config.name", "application-worker"))
                .run(args);
    }
}
