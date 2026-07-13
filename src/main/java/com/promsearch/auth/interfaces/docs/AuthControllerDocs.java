package com.promsearch.auth.interfaces.docs;

import com.promsearch.auth.interfaces.dto.LoginRequest;
import com.promsearch.auth.interfaces.dto.LoginResponse;
import com.promsearch.auth.interfaces.dto.ReissueRequest;
import com.promsearch.auth.interfaces.dto.ReissueResponse;
import com.promsearch.auth.interfaces.dto.SignupRequest;
import com.promsearch.auth.interfaces.dto.SignupResponse;
import com.promsearch.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Auth | 인증", description = "회원가입, 로그인, Access Token 재발급 API")
public interface AuthControllerDocs {

    @Operation(
            summary = "[AUTH-001] 회원가입",
            description = "이름, 닉네임, 이메일, 비밀번호로 신규 사용자를 생성합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이메일 또는 닉네임 중복")
    })
    ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request);

    @Operation(
            summary = "[AUTH-002] 로그인",
            description = "이메일과 비밀번호를 검증한 뒤 Access Token과 Refresh Token을 발급합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 불일치")
    })
    ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request);

    @Operation(
            summary = "[AUTH-003] 토큰 재발급",
            description = "유효한 Refresh Token을 검증하고 새 Access Token과 Refresh Token을 발급합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "재발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효하지 않은 Refresh Token")
    })
    ResponseEntity<ApiResponse<ReissueResponse>> reissue(@Valid @RequestBody ReissueRequest request);
}
