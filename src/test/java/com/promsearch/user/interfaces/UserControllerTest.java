package com.promsearch.user.interfaces;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.promsearch.auth.application.usecase.AuthenticateAccessTokenUseCase;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import com.promsearch.user.application.usecase.ChangePasswordUseCase;
import com.promsearch.user.application.usecase.DeleteUserUseCase;
import com.promsearch.user.application.usecase.GetUserProfileUseCase;
import com.promsearch.user.application.usecase.UpdateUserProfileUseCase;
import com.promsearch.user.application.usecase.dto.UserInfo;
import com.promsearch.user.domain.enums.UserGrade;
import com.promsearch.user.domain.enums.UserRole;
import com.promsearch.user.domain.enums.UserStatus;
import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private GetUserProfileUseCase getUserProfileUseCase;

    @MockitoBean
    private UpdateUserProfileUseCase updateUserProfileUseCase;

    @MockitoBean
    private ChangePasswordUseCase changePasswordUseCase;

    @MockitoBean
    private DeleteUserUseCase deleteUserUseCase;

    @BeforeEach
    void setUpAuthentication() {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(1L, "USER");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        ));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @DisplayName("내 프로필 조회는 닉네임, 이메일, 포인트, 등급을 반환한다")
    @Test
    void getMyProfileReturnsProfile() throws Exception {
        Instant now = Instant.now();
        given(getUserProfileUseCase.getMyProfile(1L)).willReturn(new UserInfo(
                1L,
                "user@promsearch.com",
                "hanharam",
                "한하람",
                "https://example-bucket.s3.amazonaws.com/profile/1.png",
                1200L,
                UserRole.USER,
                UserGrade.ORIGIN,
                UserStatus.ACTIVE,
                now,
                now
        ));

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result.username").value("hanharam"))
                .andExpect(jsonPath("$.result.email").value("user@promsearch.com"))
                .andExpect(jsonPath("$.result.point").value(1200))
                .andExpect(jsonPath("$.result.gradeName").value("ORIGIN"));
    }

    @DisplayName("내 프로필 조회는 사용자가 없으면 404를 반환한다")
    @Test
    void getMyProfileReturnsNotFoundWhenUserMissing() throws Exception {
        given(getUserProfileUseCase.getMyProfile(1L))
                .willThrow(new UserDomainException(UserErrorCode.USER_NOT_FOUND));

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("USER-001"));
    }
}
