package com.promsearch.global.config.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestPropertySource(properties = "SWAGGER_ENABLE=false")
class SwaggerDevDisabledContextTest {

    @Autowired
    private MockMvc mockMvc;

    @DisplayName("dev에서 Swagger 비활성화 시 Basic Auth credential 없이도 서버 컨텍스트가 시작되고 docs는 노출되지 않는다")
    @Test
    void devDisabledStartsWithoutSwaggerCredentials() throws Exception {
        mockMvc.perform(get("/docs-json"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/test/health-check"))
                .andExpect(status().isOk());
    }
}
