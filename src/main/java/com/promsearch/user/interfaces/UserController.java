package com.promsearch.user.interfaces;

import com.promsearch.global.exception.NotImplementedException;
import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import com.promsearch.user.application.usecase.CheckNicknameAvailabilityUseCase;
import com.promsearch.user.application.usecase.ChangePasswordUseCase;
import com.promsearch.user.application.usecase.CompleteProfileImageUploadUseCase;
import com.promsearch.user.application.usecase.DeleteUserUseCase;
import com.promsearch.user.application.usecase.DeleteProfileImageUseCase;
import com.promsearch.user.application.usecase.GetPublicUserProfileUseCase;
import com.promsearch.user.application.usecase.UpdateUserProfileUseCase;
import com.promsearch.user.application.usecase.IssueProfileImageUploadUrlUseCase;
import com.promsearch.user.application.usecase.dto.NicknameAvailabilityInfo;
import com.promsearch.user.application.usecase.dto.NicknameAvailabilityQuery;
import com.promsearch.user.application.usecase.dto.PublicUserProfileInfo;
import com.promsearch.user.application.usecase.dto.UserInfo;
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
import com.promsearch.user.interfaces.docs.UserControllerDocs;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 프로필과 프로필 이미지 관련 HTTP 요청을 애플리케이션 유스케이스로 전달합니다.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController implements UserControllerDocs {

    private final CheckNicknameAvailabilityUseCase checkNicknameAvailabilityUseCase;
    private final UpdateUserProfileUseCase updateUserProfileUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final DeleteUserUseCase deleteUserUseCase;
    private final GetPublicUserProfileUseCase getPublicUserProfileUseCase;
    private final IssueProfileImageUploadUrlUseCase issueProfileImageUploadUrlUseCase;
    private final CompleteProfileImageUploadUseCase completeProfileImageUploadUseCase;
    private final DeleteProfileImageUseCase deleteProfileImageUseCase;

    @GetMapping("/nicknames/availability")
    @SecurityRequirements
    @Override
    public ApiResponse<NicknameAvailabilityResponse> checkNicknameAvailability(
            @RequestParam String nickname
    ) {
        NicknameAvailabilityInfo info = checkNicknameAvailabilityUseCase.checkAvailability(
                NicknameAvailabilityQuery.of(nickname)
        );
        return ApiResponse.onSuccess(NicknameAvailabilityResponse.from(info));
    }

    @GetMapping("/me")
    @Override
    public ApiResponse<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user
    ) {
        throw new NotImplementedException();
    }

    @PatchMapping("/me")
    @Override
    public ApiResponse<UserResponse> updateProfile(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        UserInfo userInfo = updateUserProfileUseCase.updateProfile(request.toCommand(user.userId()));
        return ApiResponse.onSuccess(UserResponse.from(userInfo));
    }

    @PostMapping("/me/profile-image/upload-url")
    @Override
    public ApiResponse<ProfileImageUploadUrlResponse> issueProfileImageUploadUrl(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @Valid @RequestBody ProfileImageUploadUrlRequest request
    ) {
        return ApiResponse.onSuccess(ProfileImageUploadUrlResponse.from(
                issueProfileImageUploadUrlUseCase.issue(request.toCommand(user.userId()))
        ));
    }

    @PutMapping("/me/profile-image")
    @Override
    public ApiResponse<ProfileImageResponse> completeProfileImageUpload(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @Valid @RequestBody CompleteProfileImageUploadRequest request
    ) {
        return ApiResponse.onSuccess(ProfileImageResponse.from(
                completeProfileImageUploadUseCase.complete(request.toCommand(user.userId()))
        ));
    }

    @DeleteMapping("/me/profile-image")
    @Override
    public ApiResponse<Void> deleteProfileImage(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user
    ) {
        deleteProfileImageUseCase.delete(user.userId());
        return ApiResponse.<Void>onSuccess(null);
    }

    @PatchMapping("/me/password")
    @Override
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        changePasswordUseCase.changePassword(request.toCommand(user.userId()));
        return ApiResponse.<Void>onSuccess(null);
    }

    @DeleteMapping("/me")
    @Override
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthenticatedUserPrincipal user) {
        deleteUserUseCase.delete(user.userId());
        return ApiResponse.<Void>onSuccess(null);
    }

    @GetMapping("/{userId}/profile")
    @SecurityRequirements
    @Override
    public ApiResponse<PublicUserProfileResponse> getPublicProfile(@PathVariable @Positive Long userId) {
        PublicUserProfileInfo profile = getPublicUserProfileUseCase.getProfile(userId);
        return ApiResponse.onSuccess(PublicUserProfileResponse.from(profile));
    }
}
