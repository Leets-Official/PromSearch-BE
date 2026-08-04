package com.promsearch.user.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.promsearch.auth.application.usecase.AuthenticateAccessTokenUseCase;
import com.promsearch.user.application.usecase.CheckNicknameAvailabilityUseCase;
import com.promsearch.user.application.usecase.ChangePasswordUseCase;
import com.promsearch.user.application.usecase.DeleteUserUseCase;
import com.promsearch.user.application.usecase.GetPublicUserProfileUseCase;
import com.promsearch.user.application.usecase.GetMyProfileUseCase;
import com.promsearch.user.application.usecase.IssueProfileImageUploadUrlUseCase;
import com.promsearch.user.application.usecase.CompleteProfileImageUploadUseCase;
import com.promsearch.user.application.usecase.RemoveProfileImageUseCase;
import com.promsearch.user.application.usecase.UpdateUserProfileUseCase;
import com.promsearch.user.application.usecase.dto.NicknameAvailabilityInfo;
import com.promsearch.user.application.usecase.dto.NicknameAvailabilityQuery;
import com.promsearch.user.application.usecase.dto.PublicUserProfileInfo;
import com.promsearch.user.application.usecase.dto.ProfileImageUploadUrlInfo;
import com.promsearch.user.application.usecase.dto.UserInfo;
import com.promsearch.user.application.usecase.dto.UserProfileInfo;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import com.promsearch.user.domain.enums.UserGrade;
import com.promsearch.user.domain.enums.UserRole;
import com.promsearch.user.domain.enums.UserStatus;
import java.net.URI;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticateAccessTokenUseCase authenticateAccessTokenUseCase;

    @MockitoBean
    private CheckNicknameAvailabilityUseCase checkNicknameAvailabilityUseCase;

    @MockitoBean
    private UpdateUserProfileUseCase updateUserProfileUseCase;

    @MockitoBean
    private ChangePasswordUseCase changePasswordUseCase;

    @MockitoBean
    private DeleteUserUseCase deleteUserUseCase;

    @MockitoBean
    private GetPublicUserProfileUseCase getPublicUserProfileUseCase;

    @MockitoBean
    private GetMyProfileUseCase getMyProfileUseCase;

    @MockitoBean
    private IssueProfileImageUploadUrlUseCase issueProfileImageUploadUrlUseCase;

    @MockitoBean
    private CompleteProfileImageUploadUseCase completeProfileImageUploadUseCase;

    @MockitoBean
    private RemoveProfileImageUseCase removeProfileImageUseCase;

    @DisplayName("사용 가능한 닉네임이면 available true를 반환한다")
    @Test
    void checkNicknameAvailabilityReturnsTrue() throws Exception {
        when(checkNicknameAvailabilityUseCase.checkAvailability(any(NicknameAvailabilityQuery.class)))
                .thenReturn(new NicknameAvailabilityInfo(true));

        mockMvc.perform(get("/api/v1/users/nicknames/availability")
                        .param("nickname", "new-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.available").value(true));
    }

    @DisplayName("닉네임이 비어 있으면 요청을 거부한다")
    @Test
    void checkNicknameAvailabilityRejectsBlankNickname() throws Exception {
        mockMvc.perform(get("/api/v1/users/nicknames/availability")
                        .param("nickname", " "))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("닉네임이 100자를 초과하면 요청을 거부한다")
    @Test
    void checkNicknameAvailabilityRejectsLongNickname() throws Exception {
        mockMvc.perform(get("/api/v1/users/nicknames/availability")
                        .param("nickname", "a".repeat(101)))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("내 프로필 조회는 저장된 프로필 정보를 반환한다")
    @Test
    void getMyProfileReturnsProfile() throws Exception {
        when(getMyProfileUseCase.getMyProfile(1L)).thenReturn(new UserProfileInfo(
                "nickname",
                "https://s3.test/signed-profile.png",
                "user@test.com",
                100L,
                "NORMAL"
        ));

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUserPrincipal(1L, "USER"),
                null,
                java.util.List.of()
        ));
        try {
            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.username").value("nickname"))
                    .andExpect(jsonPath("$.result.profileImageUrl").value("https://s3.test/signed-profile.png"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @DisplayName("프로필 이미지 업로드 URL은 서명 조건과 Object Key를 반환한다")
    @Test
    void issueProfileImageUploadUrlReturnsSignedConditions() throws Exception {
        when(issueProfileImageUploadUrlUseCase.issue(any())).thenReturn(new ProfileImageUploadUrlInfo(
                "profiles/1/123e4567-e89b-12d3-a456-426614174000.jpg",
                URI.create("https://s3.test/upload"),
                "image/jpeg",
                1_024L,
                Instant.parse("2026-08-04T12:10:00Z")
        ));

        authenticate();
        try {
            mockMvc.perform(post("/api/v1/users/me/profile-image/upload-url")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"contentType":"image/jpeg","fileSize":1024}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.objectKey")
                            .value("profiles/1/123e4567-e89b-12d3-a456-426614174000.jpg"))
                    .andExpect(jsonPath("$.result.contentType").value("image/jpeg"))
                    .andExpect(jsonPath("$.result.contentLength").value(1024))
                    .andExpect(jsonPath("$.result.ifNoneMatch").value("*"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @DisplayName("업로드 완료 API는 Object Key만 받아 프로필 이미지를 교체한다")
    @Test
    void completeProfileImageUploadAcceptsObjectKeyOnly() throws Exception {
        Instant now = Instant.parse("2026-08-04T12:00:00Z");
        when(completeProfileImageUploadUseCase.complete(any())).thenReturn(new UserInfo(
                1L,
                "user@test.com",
                "nickname",
                "https://s3.test/signed-profile",
                100L,
                UserRole.USER,
                UserGrade.NORMAL,
                UserStatus.ACTIVE,
                now,
                now
        ));

        authenticate();
        try {
            mockMvc.perform(put("/api/v1/users/me/profile-image")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"objectKey":"profiles/1/123e4567-e89b-12d3-a456-426614174000.jpg"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.profileImageUrl")
                            .value("https://s3.test/signed-profile"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @DisplayName("프로필 이미지 삭제 API는 이미지가 없어도 성공한다")
    @Test
    void removeProfileImageIsIdempotent() throws Exception {
        authenticate();
        try {
            mockMvc.perform(delete("/api/v1/users/me/profile-image"))
                    .andExpect(status().isOk());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @DisplayName("공개 프로필 조회는 실명을 응답하지 않는다")
    @Test
    void getPublicProfileDoesNotExposeRealName() throws Exception {
        when(getPublicUserProfileUseCase.getProfile(12L)).thenReturn(new PublicUserProfileInfo(
                12L,
                "prompt-maker",
                "https://s3.test/signed-profiles/12.jpg",
                UserGrade.PRIME,
                8,
                124,
                2_300,
                Instant.parse("2026-07-23T12:00:00Z")
        ));

        mockMvc.perform(get("/api/v1/users/12/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.userId").value(12))
                .andExpect(jsonPath("$.result.nickname").value("prompt-maker"))
                .andExpect(jsonPath("$.result.name").doesNotExist());
    }

    private void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUserPrincipal(1L, "USER"),
                null,
                java.util.List.of()
        ));
    }
}
