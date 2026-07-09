package com.promsearch.user.interfaces;

import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.response.code.SuccessCode;
import com.promsearch.user.application.DeleteUserUseCase;
import com.promsearch.user.application.UpdateUserProfileUseCase;
import com.promsearch.user.application.UserInfo;
import com.promsearch.user.interfaces.dto.UpdateUserProfileRequest;
import com.promsearch.user.interfaces.dto.UserResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UpdateUserProfileUseCase updateUserProfileUseCase;
    private final DeleteUserUseCase deleteUserUseCase;

    @PatchMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @PathVariable @Positive Long userId,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        UserInfo userInfo = updateUserProfileUseCase.updateProfile(request.toCommand(userId));
        return ResponseEntity.ok(ApiResponse.onSuccess(UserResponse.from(userInfo)));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable @Positive Long userId) {
        deleteUserUseCase.delete(userId);
        return ResponseEntity
                .status(SuccessCode.OK.getHttpStatus())
                .body(ApiResponse.onSuccess(null));
    }
}
