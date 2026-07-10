package com.promsearch.global.config.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String JWT_BEARER_SCHEME = "jwtBearerAuth";

    @Bean
    public OpenAPI promSearchOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PromSearch API")
                        .version("v1")
                        .description("PromSearch Backend API documentation"))
                .components(new Components()
                        /* JWT 구현 전까지 문서에 인증 방식만 노출하고 전역 security requirement는 걸지 않는다. */
                        .addSecuritySchemes(JWT_BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Bearer authentication scheme for documented APIs.")));
    }
}
