package com.promsearch.global.config.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
@TestPropertySource(properties = {
        "SWAGGER_ENABLE=true",
        "SWAGGER_AUTH_USERNAME=swagger",
        "SWAGGER_AUTH_PASSWORD=secret"
})
class SwaggerDevEnabledMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @DisplayName("dev에서 Swagger 활성화 시 OpenAPI JSON은 Basic Auth로 보호된다")
    @Test
    void docsJsonProtectedByBasicAuthInDev() throws Exception {
        mockMvc.perform(get("/docs-json"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/docs-json").with(httpBasic("swagger", "secret")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("PromSearch API"));
    }

    @DisplayName("dev Swagger Basic Auth는 일반 API에 전역 적용되지 않는다")
    @Test
    void ordinaryApiIsNotProtectedBySwaggerBasicAuth() throws Exception {
        mockMvc.perform(get("/test/health-check"))
                .andExpect(status().isOk());
    }
}
