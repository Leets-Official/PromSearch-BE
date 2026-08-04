package com.promsearch.user.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import com.promsearch.auth.application.usecase.AuthenticateAccessTokenUseCase;
import com.promsearch.user.application.usecase.CheckNicknameAvailabilityUseCase;
import com.promsearch.user.application.usecase.ChangePasswordUseCase;
import com.promsearch.user.application.usecase.CompleteProfileImageUploadUseCase;
import com.promsearch.user.application.usecase.DeleteProfileImageUseCase;
import com.promsearch.user.application.usecase.DeleteUserUseCase;
import com.promsearch.user.application.usecase.GetPublicUserProfileUseCase;
import com.promsearch.user.application.usecase.IssueProfileImageUploadUrlUseCase;
import com.promsearch.user.application.usecase.UpdateUserProfileUseCase;
import com.promsearch.user.application.usecase.dto.NicknameAvailabilityInfo;
import com.promsearch.user.application.usecase.dto.NicknameAvailabilityQuery;
import com.promsearch.user.application.usecase.dto.ProfileImageInfo;
import com.promsearch.user.application.usecase.dto.ProfileImageUploadUrlInfo;
import com.promsearch.user.application.usecase.dto.PublicUserProfileInfo;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import com.promsearch.user.domain.enums.UserGrade;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

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
    private IssueProfileImageUploadUrlUseCase issueProfileImageUploadUrlUseCase;

    @MockitoBean
    private CompleteProfileImageUploadUseCase completeProfileImageUploadUseCase;

    @MockitoBean
    private DeleteProfileImageUseCase deleteProfileImageUseCase;

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

    @DisplayName("내 프로필 조회는 가짜 성공 대신 구현 중 응답을 반환한다")
    @Test
    void getMyProfileReturnsNotImplemented() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-501"));
    }

    @DisplayName("공개 프로필 조회는 실명을 응답하지 않는다")
    @Test
    void getPublicProfileDoesNotExposeRealName() throws Exception {
        when(getPublicUserProfileUseCase.getProfile(12L)).thenReturn(new PublicUserProfileInfo(
                12L,
                "prompt-maker",
                "https://cdn.promsearch.com/profiles/12.jpg",
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

    @DisplayName("프로필 이미지 업로드용 Presigned URL과 Object Key를 반환한다")
    @Test
    void issueProfileImageUploadUrl() throws Exception {
        when(issueProfileImageUploadUrlUseCase.issue(any())).thenReturn(new ProfileImageUploadUrlInfo(
                "profiles/12/123e4567-e89b-12d3-a456-426614174000.jpg",
                URI.create("https://s3.example.com/upload"),
                "image/jpeg",
                1_024L,
                Instant.parse("2026-08-04T12:10:00Z")
        ));

        mockMvc.perform(post("/api/v1/users/me/profile-image/upload-url")
                        .with(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentType":"image/jpeg","fileSize":1024}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.objectKey")
                        .value("profiles/12/123e4567-e89b-12d3-a456-426614174000.jpg"))
                .andExpect(jsonPath("$.result.uploadUrl").value("https://s3.example.com/upload"))
                .andExpect(jsonPath("$.result.contentType").value("image/jpeg"))
                .andExpect(jsonPath("$.result.contentLength").value(1024))
                .andExpect(jsonPath("$.result.ifNoneMatch").value("*"));
    }

    @DisplayName("5MB를 초과한 프로필 이미지는 URL을 발급하지 않는다")
    @Test
    void issueProfileImageUploadUrlRejectsOversizedFile() throws Exception {
        mockMvc.perform(post("/api/v1/users/me/profile-image/upload-url")
                        .with(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentType":"image/jpeg","fileSize":5242881}
                                """))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("업로드 완료 Object Key를 적용하고 최종 프로필 URL을 반환한다")
    @Test
    void completeProfileImageUpload() throws Exception {
        when(completeProfileImageUploadUseCase.complete(any()))
                .thenReturn(new ProfileImageInfo("https://cdn.example.com/profiles/12/profile.jpg"));

        mockMvc.perform(put("/api/v1/users/me/profile-image")
                        .with(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"objectKey":"profiles/12/123e4567-e89b-12d3-a456-426614174000.jpg"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.profileImageUrl")
                        .value("https://cdn.example.com/profiles/12/profile.jpg"));
    }

    @DisplayName("프로필 이미지 삭제 API는 이미지가 없어도 성공한다")
    @Test
    void deleteProfileImage() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me/profile-image")
                        .with(authenticatedUser()))
                .andExpect(status().isOk());

        verify(deleteProfileImageUseCase).delete(12L);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor authenticatedUser() {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                new AuthenticatedUserPrincipal(12L, "USER"),
                null,
                List.of()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return request -> request;
    }
}
