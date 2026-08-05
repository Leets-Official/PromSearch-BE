package com.promsearch.user.interfaces.docs;

import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import com.promsearch.user.interfaces.dto.request.ChangePasswordRequest;
import com.promsearch.user.interfaces.dto.request.UpdateUserProfileRequest;
import com.promsearch.user.interfaces.dto.request.ProfileImageUploadUrlRequest;
import com.promsearch.user.interfaces.dto.request.CompleteProfileImageUploadRequest;
import com.promsearch.user.interfaces.dto.response.NicknameAvailabilityResponse;
import com.promsearch.user.interfaces.dto.response.PublicUserProfileResponse;
import com.promsearch.user.interfaces.dto.response.UserProfileResponse;
import com.promsearch.user.interfaces.dto.response.UserResponse;
import com.promsearch.user.interfaces.dto.response.ProfileImageUploadUrlResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 사용자 API의 OpenAPI 명세를 정의합니다.
 */
@Tag(name = "User | 사용자", description = "내 프로필 조회·수정, 비밀번호 변경, 회원 탈퇴, 공개 프로필 조회 API")
public interface UserControllerDocs {

    String IMPLEMENTED_BY_HANHARAM = "**작업자: 한하람 | 구현 상태: 구현완료**\n\n";
    String IMPLEMENTED_BY_RUCHAN04 = "**작업자: ruchan04 | 구현 상태: 구현완료**\n\n";

    @Operation(
            summary = "[USER-005] 닉네임 중복 확인",
            description = IMPLEMENTED_BY_HANHARAM
                    + "회원가입 또는 프로필 수정 전에 닉네임 사용 가능 여부를 확인합니다. "
                    + "응답은 안내용이며 실제 저장 시 서버에서 중복 여부를 다시 검증합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "닉네임 사용 가능 여부 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "닉네임 누락 또는 길이 제한 초과")
    })
    ApiResponse<NicknameAvailabilityResponse> checkNicknameAvailability(
            @Parameter(description = "확인할 닉네임", required = true, example = "prompt-master")
            @RequestParam
            @NotBlank(message = "닉네임은 필수입니다.")
            @Size(max = 100, message = "닉네임은 100자 이하여야 합니다.")
            String nickname
    );

    @Operation(
            summary = "[USER-004] 내 프로필 조회",
            description = IMPLEMENTED_BY_HANHARAM
                    + "인증된 사용자의 마이페이지 프로필 정보를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로필 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    ApiResponse<UserProfileResponse> getMyProfile(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user
    );

    @Operation(
            summary = "[USER-001] 내 프로필 수정",
            description = IMPLEMENTED_BY_RUCHAN04
                    + "인증된 사용자의 이름, 닉네임, 이메일, 외부 프로필 이미지 URL과 관심 태그를 수정합니다. "
                    + "관심 직군과 관심 태스크는 각각 최대 3개이며, 각 배열이 null이면 해당 기존 선택을 유지합니다."
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
            summary = "[USER-007] 프로필 이미지 업로드 URL 발급",
            description = IMPLEMENTED_BY_HANHARAM
                    + "JPEG, PNG, WEBP 프로필 이미지를 S3에 직접 업로드할 수 있는 Presigned PUT URL을 발급합니다. "
                    + "응답의 Content-Type, Content-Length, If-None-Match 조건을 지켜 PUT 요청해야 합니다."
    )
    ApiResponse<ProfileImageUploadUrlResponse> issueProfileImageUploadUrl(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @Valid @RequestBody ProfileImageUploadUrlRequest request
    );

    @Operation(
            summary = "[USER-008] 프로필 이미지 업로드 완료",
            description = IMPLEMENTED_BY_HANHARAM
                    + "발급 응답의 Object Key 소유권과 실제 S3 객체 메타데이터를 검증한 후 사용자 프로필 이미지로 연결합니다."
    )
    ApiResponse<UserResponse> completeProfileImageUpload(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @Valid @RequestBody CompleteProfileImageUploadRequest request
    );

    @Operation(
            summary = "[USER-009] 프로필 이미지 제거",
            description = IMPLEMENTED_BY_HANHARAM
                    + "프로필 이미지를 제거하고 S3 관리 객체는 트랜잭션 커밋 후 정리합니다."
    )
    ApiResponse<Void> removeProfileImage(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUserPrincipal user
    );

    @Operation(
            summary = "[USER-002] 내 비밀번호 변경",
            description = IMPLEMENTED_BY_RUCHAN04
                    + "현재 비밀번호를 확인한 뒤 새 비밀번호로 변경합니다."
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
            description = IMPLEMENTED_BY_RUCHAN04
                    + "인증된 사용자를 논리 삭제하고 재가입 허용을 위해 재사용 가능한 개인정보를 익명화합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회원 탈퇴 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    ApiResponse<Void> delete(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user
    );

    @Operation(
            summary = "[USER-006] 상대 프로필 조회",
            description = IMPLEMENTED_BY_RUCHAN04
                    + "프롬프트 카드에서 작성자 프로필을 열 때 필요한 공개 프로필 정보만 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상대 프로필 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "사용자 ID 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자 없음")
    })
    ApiResponse<PublicUserProfileResponse> getPublicProfile(
            @Parameter(description = "조회할 사용자 ID", example = "12", required = true)
            @PathVariable @Positive Long userId
    );
}
