package com.promsearch.user.interfaces;

import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import com.promsearch.user.application.usecase.ChangePasswordUseCase;
import com.promsearch.user.application.usecase.DeleteUserUseCase;
import com.promsearch.user.application.usecase.UpdateUserProfileUseCase;
import com.promsearch.user.application.usecase.dto.UserInfo;
import com.promsearch.user.interfaces.dto.request.ChangePasswordRequest;
import com.promsearch.user.interfaces.dto.request.UpdateUserProfileRequest;
import com.promsearch.user.interfaces.dto.response.UserResponse;
import com.promsearch.user.interfaces.docs.UserControllerDocs;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
}
