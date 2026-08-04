package com.promsearch.user.interfaces.docs;

import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import com.promsearch.user.interfaces.dto.request.ChangePasswordRequest;
import com.promsearch.user.interfaces.dto.request.CompleteProfileImageUploadRequest;
import com.promsearch.user.interfaces.dto.request.ProfileImageUploadUrlRequest;
import com.promsearch.user.interfaces.dto.request.UpdateUserProfileRequest;
import com.promsearch.user.interfaces.dto.response.NicknameAvailabilityResponse;
import com.promsearch.user.interfaces.dto.response.ProfileImageResponse;
import com.promsearch.user.interfaces.dto.response.ProfileImageUploadUrlResponse;
import com.promsearch.user.interfaces.dto.response.PublicUserProfileResponse;
import com.promsearch.user.interfaces.dto.response.UserProfileResponse;
import com.promsearch.user.interfaces.dto.response.UserResponse;
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
 * 사용자 API의 OpenAPI 계약과 공개 메서드 문서를 정의합니다.
 */
@Tag(name = "User | 사용자", description = "내 프로필 조회·수정, 프로필 이미지, 비밀번호 변경, 회원 탈퇴, 공개 프로필 조회 API")
public interface UserControllerDocs {

    String IMPLEMENTED_BY_HANHARAM = "**작업자: 한하람 | 구현 상태: 구현완료**\n\n";
    String NOT_IMPLEMENTED_BY_KALLIN1 = "**작업자: kallin1 | 구현 상태: 미구현**\n\n";
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
            description = NOT_IMPLEMENTED_BY_KALLIN1
                    + "인증된 사용자의 마이페이지 프로필 정보를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로필 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "501", description = "인터페이스 계약만 작성되어 실제 조회 기능은 구현 중")
    })
    ApiResponse<UserProfileResponse> getMyProfile(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user
    );

    @Operation(
            summary = "[USER-001] 내 프로필 수정",
            description = IMPLEMENTED_BY_RUCHAN04
                    + "인증된 사용자의 이름, 닉네임, 이메일을 수정합니다. 프로필 이미지는 전용 API를 사용합니다."
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

    /**
     * 프로필 이미지의 S3 직접 업로드 URL과 사용자 전용 Object Key를 발급합니다.
     *
     * @param user 인증된 사용자
     * @param request 업로드할 이미지의 MIME 타입과 파일 크기
     * @return Presigned PUT URL과 업로드 조건
     */
    @Operation(
            summary = "[USER-007] 프로필 이미지 업로드 URL 발급",
            description = IMPLEMENTED_BY_HANHARAM
                    + "JPEG 또는 PNG 프로필 이미지를 S3에 직접 PUT할 수 있는 임시 URL을 발급합니다. "
                    + "응답의 Content-Type, Content-Length, If-None-Match 조건으로 PUT 요청을 보내고, "
                    + "업로드 후 완료 API를 호출해야 합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "업로드 URL 발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "파일 형식 또는 크기 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "S3 연결 또는 자격 증명 오류")
    })
    ApiResponse<ProfileImageUploadUrlResponse> issueProfileImageUploadUrl(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,

            @Valid @RequestBody ProfileImageUploadUrlRequest request
    );

    /**
     * S3에 업로드된 객체를 검증하고 현재 프로필 이미지로 적용합니다.
     *
     * @param user 인증된 사용자
     * @param request 업로드 URL 발급 응답에서 받은 Object Key
     * @return 적용된 프로필 이미지 공개 URL
     */
    @Operation(
            summary = "[USER-008] 프로필 이미지 업로드 완료·교체",
            description = IMPLEMENTED_BY_HANHARAM
                    + "발급 응답의 Object Key 소유권과 S3 객체의 형식·최대 5MB 크기를 검증한 뒤 프로필에 적용합니다. "
                    + "기존 자사 S3 프로필 이미지는 DB 커밋 후 삭제합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로필 이미지 적용 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Object Key 또는 업로드 메타데이터 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "다른 사용자 경로의 Object Key"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "S3 업로드 객체 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "S3 연결 오류")
    })
    ApiResponse<ProfileImageResponse> completeProfileImageUpload(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,

            @Valid @RequestBody CompleteProfileImageUploadRequest request
    );

    /**
     * 현재 프로필 이미지 연결과 자사 S3 객체를 제거합니다.
     *
     * @param user 인증된 사용자
     * @return 성공 응답
     */
    @Operation(
            summary = "[USER-009] 프로필 이미지 삭제",
            description = IMPLEMENTED_BY_HANHARAM
                    + "프로필 이미지 연결을 제거하고 자사 S3 이미지라면 DB 커밋 후 객체를 삭제합니다. "
                    + "이미지가 없어도 성공하는 멱등 API입니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로필 이미지 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    ApiResponse<Void> deleteProfileImage(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user
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
