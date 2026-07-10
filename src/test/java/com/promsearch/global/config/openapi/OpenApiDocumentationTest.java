package com.promsearch.global.config.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OpenApiDocumentationTest {

    @DisplayName("PromSearch API 문서 메타데이터와 JWT Bearer 스키마를 등록한다")
    @Test
    void openApiMetadataAndJwtBearerScheme() {
        OpenAPI openAPI = new OpenApiConfig().promSearchOpenAPI();

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("PromSearch API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("v1");
        assertThat(openAPI.getComponents().getSecuritySchemes())
                .containsKey("jwtBearerAuth");
        assertThat(openAPI.getSecurity())
                .as("JWT 구현 전까지 모든 API를 JWT-required로 보이게 하는 전역 security requirement는 등록하지 않는다")
                .isNull();

        SecurityScheme jwtBearer = openAPI.getComponents().getSecuritySchemes().get("jwtBearerAuth");
        assertThat(jwtBearer.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(jwtBearer.getScheme()).isEqualTo("bearer");
        assertThat(jwtBearer.getBearerFormat()).isEqualTo("JWT");
    }
}
