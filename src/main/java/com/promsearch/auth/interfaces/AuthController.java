package com.promsearch.auth.interfaces;

import com.promsearch.auth.application.usecase.LoginUseCase;
import com.promsearch.auth.application.usecase.ReissueUseCase;
import com.promsearch.auth.application.usecase.SocialLoginUseCase;
import com.promsearch.auth.interfaces.dto.request.LoginRequest;
import com.promsearch.auth.interfaces.dto.response.LoginResponse;
import com.promsearch.auth.interfaces.dto.request.ReissueRequest;
import com.promsearch.auth.interfaces.dto.response.ReissueResponse;
import com.promsearch.auth.interfaces.dto.request.SignupRequest;
import com.promsearch.auth.interfaces.dto.request.SocialLoginRequest;
import com.promsearch.auth.interfaces.docs.AuthControllerDocs;
import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.response.code.SuccessCode;
import com.promsearch.user.application.usecase.SignupUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController implements AuthControllerDocs {

    private final SignupUseCase signupUseCase;
    private final LoginUseCase loginUseCase;
    private final ReissueUseCase reissueUseCase;
    private final SocialLoginUseCase socialLoginUseCase;

    @PostMapping("/signup")
    @Override
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest request) {
        signupUseCase.signup(request.toCommand());
        ApiResponse<Void> response = ApiResponse.onSuccess(SuccessCode.CREATED, null);

        return ResponseEntity
                .status(SuccessCode.CREATED.getHttpStatus())
                .body(response);
    }

    @PostMapping("/login")
    @Override
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        ApiResponse<LoginResponse> response = ApiResponse.onSuccess(
                SuccessCode.OK,
                LoginResponse.from(loginUseCase.login(request.toCommand()))
        );

        return ResponseEntity
                .status(SuccessCode.OK.getHttpStatus())
                .body(response);
    }

    @PostMapping("/reissue")
    @Override
    public ResponseEntity<ApiResponse<ReissueResponse>> reissue(@Valid @RequestBody ReissueRequest request) {
        ApiResponse<ReissueResponse> response = ApiResponse.onSuccess(
                SuccessCode.OK,
                ReissueResponse.from(reissueUseCase.reissue(request.toCommand()))
        );

        return ResponseEntity
                .status(SuccessCode.OK.getHttpStatus())
                .body(response);
    }

    @PostMapping("/oauth/{provider}")
    @Override
    public ResponseEntity<ApiResponse<LoginResponse>> socialLogin(
            @PathVariable String provider,
            @Valid @RequestBody SocialLoginRequest request
    ) {
        ApiResponse<LoginResponse> response = ApiResponse.onSuccess(
                SuccessCode.OK,
                LoginResponse.from(socialLoginUseCase.socialLogin(request.toCommand(provider)))
        );

        return ResponseEntity
                .status(SuccessCode.OK.getHttpStatus())
                .body(response);
    }
}
