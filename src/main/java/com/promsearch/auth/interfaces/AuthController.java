package com.promsearch.auth.interfaces;

import com.promsearch.auth.application.LoginUseCase;
import com.promsearch.auth.interfaces.dto.LoginRequest;
import com.promsearch.auth.interfaces.dto.LoginResponse;
import com.promsearch.auth.interfaces.dto.SignupRequest;
import com.promsearch.auth.interfaces.dto.SignupResponse;
import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.response.code.SuccessCode;
import com.promsearch.user.application.SignupUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final SignupUseCase signupUseCase;
    private final LoginUseCase loginUseCase;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        ApiResponse<SignupResponse> response = ApiResponse.onSuccess(
                SuccessCode.CREATED,
                SignupResponse.from(signupUseCase.signup(request.toCommand()))
        );

        return ResponseEntity
                .status(SuccessCode.CREATED.getHttpStatus())
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        ApiResponse<LoginResponse> response = ApiResponse.onSuccess(
                SuccessCode.OK,
                LoginResponse.from(loginUseCase.login(request.toCommand()))
        );

        return ResponseEntity
                .status(SuccessCode.OK.getHttpStatus())
                .body(response);
    }
}
