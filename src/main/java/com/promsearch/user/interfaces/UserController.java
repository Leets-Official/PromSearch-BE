package com.promsearch.user.interfaces;

import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import com.promsearch.user.application.ChangePasswordUseCase;
import com.promsearch.user.application.DeleteUserUseCase;
import com.promsearch.user.application.GetPublicUserProfileUseCase;
import com.promsearch.user.application.PublicUserProfileInfo;
import com.promsearch.user.application.UpdateUserProfileUseCase;
import com.promsearch.user.application.UserInfo;
import com.promsearch.user.interfaces.dto.ChangePasswordRequest;
import com.promsearch.user.interfaces.dto.PublicUserProfileResponse;
import com.promsearch.user.interfaces.dto.UpdateUserProfileRequest;
import com.promsearch.user.interfaces.dto.UserResponse;
import com.promsearch.user.interfaces.docs.UserControllerDocs;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController implements UserControllerDocs {

    private final UpdateUserProfileUseCase updateUserProfileUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final DeleteUserUseCase deleteUserUseCase;
    private final GetPublicUserProfileUseCase getPublicUserProfileUseCase;

    @PatchMapping("/me")
    @Override
    public ApiResponse<UserResponse> updateProfile(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        UserInfo userInfo = updateUserProfileUseCase.updateProfile(request.toCommand(user.userId()));
        return ApiResponse.onSuccess(UserResponse.from(userInfo));
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
    @Override
    public ApiResponse<PublicUserProfileResponse> getPublicProfile(@PathVariable @Positive Long userId) {
        PublicUserProfileInfo profile = getPublicUserProfileUseCase.getProfile(userId);
        return ApiResponse.onSuccess(PublicUserProfileResponse.from(profile));
    }
}
