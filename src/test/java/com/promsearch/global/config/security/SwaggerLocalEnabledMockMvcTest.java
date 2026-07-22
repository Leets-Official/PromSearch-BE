package com.promsearch.global.config.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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
@ActiveProfiles("local")
@TestPropertySource(properties = "SWAGGER_ENABLE=true")
class SwaggerLocalEnabledMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @DisplayName("local에서 Swagger 활성화 시 OpenAPI JSON은 Basic Auth 없이 접근 가능하다")
    @Test
    void docsJsonAccessibleWithoutBasicAuthInLocal() throws Exception {
        mockMvc.perform(get("/docs-json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("PromSearch API"))
                .andExpect(jsonPath("$.components.securitySchemes.jwtBearerAuth.scheme").value("bearer"));
    }

    @DisplayName("local에서 Swagger UI는 /docs 경로로 노출된다")
    @Test
    void swaggerUiServedAtDocsPathInLocal() throws Exception {
        mockMvc.perform(get("/docs"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));
    }
}
