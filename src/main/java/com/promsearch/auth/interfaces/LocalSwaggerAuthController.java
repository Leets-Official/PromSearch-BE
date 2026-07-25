package com.promsearch.auth.interfaces;

import com.promsearch.auth.application.usecase.dto.AuthenticatedUserInfo;
import com.promsearch.auth.application.usecase.IssueLocalSwaggerTokenUseCase;
import com.promsearch.auth.application.usecase.dto.LocalSwaggerTokenInfo;
import com.promsearch.auth.interfaces.dto.request.SwaggerTokenRequest;
import com.promsearch.auth.interfaces.dto.response.SwaggerTokenResponse;
import com.promsearch.auth.interfaces.docs.LocalSwaggerAuthControllerDocs;
import com.promsearch.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("local")
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class LocalSwaggerAuthController implements LocalSwaggerAuthControllerDocs {

    private final IssueLocalSwaggerTokenUseCase issueLocalSwaggerTokenUseCase;

    @PostMapping("/swagger-token")
    @Override
    public ApiResponse<SwaggerTokenResponse> createSwaggerToken(
            @Valid @RequestBody(required = false) SwaggerTokenRequest request
    ) {
        AuthenticatedUserInfo user = SwaggerTokenRequest.toAuthenticatedUser(request);
        LocalSwaggerTokenInfo tokenInfo = issueLocalSwaggerTokenUseCase.issue(user);
        return ApiResponse.onSuccess(SwaggerTokenResponse.from(tokenInfo));
    }
}
