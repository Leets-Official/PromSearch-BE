package com.promsearch.auth.interfaces.docs;

import com.promsearch.auth.interfaces.dto.request.SwaggerTokenRequest;
import com.promsearch.auth.interfaces.dto.response.SwaggerTokenResponse;
import com.promsearch.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Local Dev | Swagger 인증", description = "local profile 전용 Swagger 테스트 토큰 발급 API")
public interface LocalSwaggerAuthControllerDocs {

    @Operation(
            summary = "[LOCAL-001] Swagger 테스트 토큰 발급",
            description = """
                    local profile에서만 활성화됩니다.
                    요청 본문을 비우면 기본 사용자(userId=1, role=USER) 기준으로 Access Token을 발급합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "테스트 토큰 발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패")
    })
    ApiResponse<SwaggerTokenResponse> createSwaggerToken(
            @Valid @RequestBody(required = false) SwaggerTokenRequest request
    );
}
