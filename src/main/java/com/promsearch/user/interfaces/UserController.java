package com.promsearch.user.interfaces;

import com.promsearch.global.response.ApiResponse;
import com.promsearch.user.application.ChangePasswordUseCase;
import com.promsearch.user.application.DeleteUserUseCase;
import com.promsearch.user.application.UpdateUserProfileUseCase;
import com.promsearch.user.application.UserInfo;
import com.promsearch.user.interfaces.dto.ChangePasswordRequest;
import com.promsearch.user.interfaces.dto.UpdateUserProfileRequest;
import com.promsearch.user.interfaces.dto.UserResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private static final String AUTHENTICATED_USER_ID_HEADER = "X-User-Id";

    private final UpdateUserProfileUseCase updateUserProfileUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final DeleteUserUseCase deleteUserUseCase;

    @PatchMapping("/me")
    public ApiResponse<UserResponse> updateProfile(
            @RequestHeader(AUTHENTICATED_USER_ID_HEADER) @Positive Long userId,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        UserInfo userInfo = updateUserProfileUseCase.updateProfile(request.toCommand(userId));
        return ApiResponse.onSuccess(UserResponse.from(userInfo));
    }

    @PatchMapping("/me/password")
    public ApiResponse<Void> changePassword(
            @RequestHeader(AUTHENTICATED_USER_ID_HEADER) @Positive Long userId,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        changePasswordUseCase.changePassword(request.toCommand(userId));
        return ApiResponse.<Void>onSuccess(null);
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> delete(@RequestHeader(AUTHENTICATED_USER_ID_HEADER) @Positive Long userId) {
        deleteUserUseCase.delete(userId);
        return ApiResponse.<Void>onSuccess(null);
    }
}
