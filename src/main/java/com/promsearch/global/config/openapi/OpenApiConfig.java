package com.promsearch.global.config.openapi;

import com.promsearch.auth.interfaces.AuthController;
import com.promsearch.auth.interfaces.LocalSwaggerAuthController;
import com.promsearch.community.interfaces.CommentController;
import com.promsearch.test.interfaces.TestController;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import org.springdoc.core.customizers.OperationCustomizer;
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
                        .description("PromSearch 백엔드 API 문서입니다. local 환경에서는 `/api/v1/auth/swagger-token`으로 Swagger 테스트용 Bearer 토큰을 발급할 수 있습니다."))
                .components(new Components()
                        .addSecuritySchemes(JWT_BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT Bearer authentication scheme for documented APIs.")));
    }

    @Bean
    public OperationCustomizer jwtSecurityOperationCustomizer() {
        return (operation, handlerMethod) -> {
            Class<?> beanType = handlerMethod.getBeanType();
            if (AuthController.class.isAssignableFrom(beanType)
                    || LocalSwaggerAuthController.class.isAssignableFrom(beanType)
                    || TestController.class.isAssignableFrom(beanType)) {
                return operation;
            }

            boolean commentListEndpoint = CommentController.class.isAssignableFrom(beanType)
                    && handlerMethod.getMethod().getName().equals("getComments");
            if (commentListEndpoint) {
                operation.setSecurity(List.of(
                        new SecurityRequirement(),
                        new SecurityRequirement().addList(JWT_BEARER_SCHEME)
                ));
                return operation;
            }

            return operation.addSecurityItem(new SecurityRequirement().addList(JWT_BEARER_SCHEME));
        };
    }
}
