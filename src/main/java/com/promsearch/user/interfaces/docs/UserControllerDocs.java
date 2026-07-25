package com.promsearch.user.interfaces.docs;

import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import com.promsearch.user.interfaces.dto.ChangePasswordRequest;
import com.promsearch.user.interfaces.dto.PublicUserProfileResponse;
import com.promsearch.user.interfaces.dto.UpdateUserProfileRequest;
import com.promsearch.user.interfaces.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "User", description = "Profile update, password change, withdrawal, and public profile APIs")
public interface UserControllerDocs {

    @Operation(
            summary = "[USER-001] Update my profile",
            description = "Updates the authenticated user's name, nickname, email, and profile image."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Duplicated email or nickname")
    })
    ApiResponse<UserResponse> updateProfile(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,

            @Valid @RequestBody UpdateUserProfileRequest request
    );

    @Operation(
            summary = "[USER-002] Change my password",
            description = "Verifies the current password and replaces it with a new password."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password changed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or wrong current password"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    ApiResponse<Void> changePassword(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,

            @Valid @RequestBody ChangePasswordRequest request
    );

    @Operation(
            summary = "[USER-003] Withdraw my account",
            description = "Soft-deletes the authenticated user and anonymizes reusable personal information."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User withdrawn"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    ApiResponse<Void> delete(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user
    );

    @Operation(
            summary = "[USER-004] Get public user profile",
            description = "Returns only public creator profile data for the profile page opened from a prompt card."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Public profile found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid user id"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    ApiResponse<PublicUserProfileResponse> getPublicProfile(
            @Parameter(description = "Target user id", example = "12", required = true)
            @PathVariable @Positive Long userId
    );
}
