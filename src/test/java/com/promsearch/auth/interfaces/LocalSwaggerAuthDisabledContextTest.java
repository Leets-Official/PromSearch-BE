package com.promsearch.auth.interfaces;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("local")
@TestPropertySource(properties = "SWAGGER_ENABLE=false")
class LocalSwaggerAuthDisabledContextTest {

    @Autowired
    private ApplicationContext applicationContext;

    @DisplayName("local이어도 Swagger가 비활성화되면 테스트 토큰 발급 API를 등록하지 않는다")
    @Test
    void swaggerTokenControllerIsNotRegisteredWhenSwaggerIsDisabled() {
        assertThat(applicationContext.getBeansOfType(LocalSwaggerAuthController.class)).isEmpty();
    }
}
