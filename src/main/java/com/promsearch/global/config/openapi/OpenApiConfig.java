package com.promsearch.global.config.openapi;

import com.promsearch.auth.interfaces.AuthController;
import com.promsearch.auth.interfaces.LocalSwaggerAuthController;
import com.promsearch.prompt.interfaces.HomeController;
import com.promsearch.test.interfaces.TestController;
import com.promsearch.user.interfaces.UserController;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
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
                                .description("문서화된 보호 API에서 사용하는 JWT Bearer 인증 방식입니다.")));
    }

    @Bean
    public OperationCustomizer jwtSecurityOperationCustomizer() {
        return (operation, handlerMethod) -> {
            Class<?> beanType = handlerMethod.getBeanType();
            /*
             * 실제 SecurityFilterChain에서 permitAll로 열어둔 API는 Swagger 문서에서도
             * 자물쇠 표시가 없어야 프론트/QA가 인증 필요 여부를 잘못 이해하지 않습니다.
             *
             * - Auth/Test/Swagger 토큰 API: 기존 공개 API
             * - HomeController: 홈 목록은 비회원도 접근 가능
             * - UserController#getPublicProfile: 카드 작성자 프로필은 비회원도 접근 가능
             */
            if (AuthController.class.isAssignableFrom(beanType)
                    || LocalSwaggerAuthController.class.isAssignableFrom(beanType)
                    || TestController.class.isAssignableFrom(beanType)
                    || HomeController.class.isAssignableFrom(beanType)
                    || isPublicUserProfileOperation(beanType, handlerMethod.getMethod().getName())) {
                return operation;
            }
            return operation.addSecurityItem(new SecurityRequirement().addList(JWT_BEARER_SCHEME));
        };
    }

    private boolean isPublicUserProfileOperation(Class<?> beanType, String methodName) {
        /*
         * UserController 전체를 공개 처리하면 /me 수정, 비밀번호 변경, 회원 탈퇴까지 공개 API처럼 보입니다.
         * 그래서 상대 프로필 조회 메서드만 메서드명으로 좁혀 Swagger 보안 요구사항을 제거합니다.
         */
        return UserController.class.isAssignableFrom(beanType) && "getPublicProfile".equals(methodName);
    }
}
