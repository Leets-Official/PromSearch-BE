package com.promsearch.global.config.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.promsearch.auth.interfaces.AuthController;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import com.promsearch.user.interfaces.UserController;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.web.method.HandlerMethod;

class OpenApiDocumentationTest {

    private OpenApiConfig openApiConfig;

    @BeforeEach
    void setUp() {
        openApiConfig = new OpenApiConfig();
    }

    @DisplayName("PromSearch API 문서 메타데이터와 JWT Bearer 스키마를 등록한다")
    @Test
    void openApiMetadataAndJwtBearerScheme() {
        OpenAPI openAPI = openApiConfig.promSearchOpenAPI();

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("PromSearch API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("v1");
        assertThat(openAPI.getComponents().getSecuritySchemes())
                .containsKey("jwtBearerAuth");

        SecurityScheme jwtBearer = openAPI.getComponents().getSecuritySchemes().get("jwtBearerAuth");
        assertThat(jwtBearer.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(jwtBearer.getScheme()).isEqualTo("bearer");
        assertThat(jwtBearer.getBearerFormat()).isEqualTo("JWT");
    }

    @DisplayName("보호 API에는 JWT security requirement를 문서화하고 auth API에는 걸지 않는다")
    @Test
    void jwtSecurityRequirementOnlyForProtectedApis() throws Exception {
        OperationCustomizer customizer = openApiConfig.jwtSecurityOperationCustomizer();
        Operation protectedOperation = customizer.customize(new Operation(), handlerMethod(
                new UserController(null, null, null),
                UserController.class.getMethod("delete", AuthenticatedUserPrincipal.class)
        ));
        Operation authOperation = customizer.customize(new Operation(), handlerMethod(
                new AuthController(null, null, null, null),
                AuthController.class.getMethod("login", com.promsearch.auth.interfaces.dto.LoginRequest.class)
        ));

        assertThat(protectedOperation.getSecurity())
                .flatExtracting(SecurityRequirement::keySet)
                .containsExactly("jwtBearerAuth");
        assertThat(authOperation.getSecurity()).isNull();
    }

    private HandlerMethod handlerMethod(Object bean, Method method) {
        return new HandlerMethod(bean, method);
    }
}
