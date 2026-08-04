package com.promsearch.auth.interfaces.docs;

import com.promsearch.auth.interfaces.dto.request.LoginRequest;
import com.promsearch.auth.interfaces.dto.response.LoginResponse;
import com.promsearch.auth.interfaces.dto.request.ReissueRequest;
import com.promsearch.auth.interfaces.dto.response.ReissueResponse;
import com.promsearch.auth.interfaces.dto.request.SignupRequest;
import com.promsearch.auth.interfaces.dto.request.SocialLoginRequest;
import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(name = "Auth | 인증", description = "회원가입, 로그인, Access Token 재발급 API")
public interface AuthControllerDocs {

    String IMPLEMENTED_BY_LEE_GUNHEE = "**작업자: 이건희 | 구현 상태: 구현완료**\n\n";
    String IMPLEMENTED_BY_KALLIN1 = "**작업자: kallin1 | 구현 상태: 구현완료**\n\n";

    @Operation(
            summary = "[AUTH-001] 회원가입",
            description = IMPLEMENTED_BY_LEE_GUNHEE
                    + "이메일, 비밀번호, 닉네임으로 신규 사용자를 생성합니다. "
                    + "프로필 이미지는 선택 사항이며, 관심 직군과 관심 태스크는 태그 ID로 각각 최대 3개까지 선택할 수 있습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이메일 또는 닉네임 중복")
    })
    ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest request);

    @Operation(
            summary = "[AUTH-002] 로그인",
            description = IMPLEMENTED_BY_LEE_GUNHEE
                    + "이메일과 비밀번호를 검증한 뒤 Access Token과 Refresh Token을 발급합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 불일치")
    })
    ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request);

    @Operation(
            summary = "[AUTH-003] 토큰 재발급",
            description = IMPLEMENTED_BY_LEE_GUNHEE
                    + "유효한 Refresh Token을 검증하고 새 Access Token과 Refresh Token을 발급합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "재발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효하지 않은 Refresh Token")
    })
    ResponseEntity<ApiResponse<ReissueResponse>> reissue(@Valid @RequestBody ReissueRequest request);

    @Operation(
            summary = "[AUTH-005] 로그아웃",
            description = "**작업자: Hanharam | 구현 상태: 구현완료**\n\n"
                    + "인증된 사용자의 서버 저장 Refresh Token 세션을 모두 폐기합니다."
    )
    @SecurityRequirement(name = "jwtBearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    ResponseEntity<ApiResponse<Void>> logout(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUserPrincipal user
    );

    @Operation(
            summary = "[AUTH-004] 소셜 로그인",
            description = IMPLEMENTED_BY_KALLIN1
                    + "프론트엔드에서 전달받은 OAuth 인가 코드로 소셜 로그인 또는 자동 회원가입을 수행하고 "
                    + "Access Token과 Refresh Token을 발급하며, 자동 회원가입 여부는 isNewUser로 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "소셜 로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패 또는 지원하지 않는 제공자"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "소셜 인증 실패")
    })
    ResponseEntity<ApiResponse<LoginResponse>> socialLogin(
            @Parameter(description = "소셜 로그인 제공자", example = "kakao") @PathVariable String provider,
            @Valid @RequestBody SocialLoginRequest request
    );
}
