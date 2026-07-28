package com.promsearch.user.interfaces.docs;

import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import com.promsearch.user.interfaces.dto.request.ChangePasswordRequest;
import com.promsearch.user.interfaces.dto.request.UpdateUserProfileRequest;
import com.promsearch.user.interfaces.dto.response.UserProfileResponse;
import com.promsearch.user.interfaces.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "User | 사용자", description = "내 프로필 조회·수정, 비밀번호 변경, 회원 탈퇴 API")
public interface UserControllerDocs {

    @Operation(
            summary = "[USER-004] 내 프로필 조회",
            description = "인증된 사용자의 마이페이지 프로필 정보를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로필 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없거나 탈퇴한 사용자")
    })
    ApiResponse<UserProfileResponse> getMyProfile(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user
    );

    @Operation(
            summary = "[USER-001] 내 프로필 수정",
            description = "인증된 사용자의 이름, 닉네임, 이메일, 프로필 이미지를 부분 수정합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로필 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이메일 또는 닉네임 중복")
    })
    ApiResponse<UserResponse> updateProfile(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,

            @Valid @RequestBody UpdateUserProfileRequest request
    );

    @Operation(
            summary = "[USER-002] 내 비밀번호 변경",
            description = "현재 비밀번호를 확인한 뒤 새 비밀번호로 변경합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패 또는 현재 비밀번호 불일치"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    ApiResponse<Void> changePassword(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,

            @Valid @RequestBody ChangePasswordRequest request
    );

    @Operation(
            summary = "[USER-003] 회원 탈퇴",
            description = "인증된 사용자를 논리 삭제 상태로 변경합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회원 탈퇴 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    ApiResponse<Void> delete(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user
    );
}
