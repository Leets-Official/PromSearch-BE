package com.promsearch.auth.interfaces;

import static org.hamcrest.Matchers.notNullValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promsearch.auth.application.port.out.oauth.SocialLoginResult;
import com.promsearch.auth.domain.enums.SocialProvider;
import com.promsearch.auth.domain.exception.AuthDomainException;
import com.promsearch.auth.domain.exception.AuthErrorCode;
import com.promsearch.auth.infrastructure.external.oauth.GoogleOAuthAdapter;
import com.promsearch.auth.infrastructure.external.oauth.KakaoOAuthAdapter;
import com.promsearch.auth.interfaces.dto.request.LoginRequest;
import com.promsearch.auth.interfaces.dto.request.ReissueRequest;
import com.promsearch.auth.interfaces.dto.request.SignupRequest;
import com.promsearch.auth.interfaces.dto.request.SocialLoginRequest;
import com.promsearch.user.interfaces.dto.request.ChangePasswordRequest;
import com.promsearch.user.interfaces.dto.request.UpdateUserProfileRequest;
import com.promsearch.user.infrastructure.persistence.UserRepository;
import com.promsearch.user.infrastructure.persistence.entity.UserJpaEntity;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userJpaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private KakaoOAuthAdapter kakaoOAuthAdapter;

    @MockitoBean
    private GoogleOAuthAdapter googleOAuthAdapter;

    @DisplayName("회원가입에 성공한다")
    @Test
    void signupSuccess() throws Exception {
        SignupRequest request = new SignupRequest(
                "gildong",
                "gildong@example.com",
                "password123"
        );

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("COMMON-201"))
                .andExpect(jsonPath("$.result").doesNotExist())
                .andExpect(jsonPath("$.result.password").doesNotExist());

        UserJpaEntity savedUser = userJpaRepository.findByEmail("gildong@example.com").orElseThrow();
        assertThat(savedUser.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", savedUser.getPassword())).isTrue();
    }

    @DisplayName("이미 사용 중인 닉네임이면 회원가입에 실패한다")
    @Test
    void signupFailWhenNicknameDuplicated() throws Exception {
        signup("gildong", "gildong@example.com", "password123");

        SignupRequest duplicatedNicknameRequest = new SignupRequest(
                "gildong",
                "another@example.com",
                "password123"
        );

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicatedNicknameRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("USER-003"));
    }

    @DisplayName("이미 사용 중인 이메일이면 회원가입에 실패한다")
    @Test
    void signupFailWhenEmailDuplicated() throws Exception {
        signup("gildong", "gildong@example.com", "password123");

        SignupRequest duplicatedEmailRequest = new SignupRequest(
                "another",
                "gildong@example.com",
                "password123"
        );

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicatedEmailRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("USER-002"));
    }


    @DisplayName("이메일과 닉네임이 모두 중복이면 이메일 중복 에러를 우선 반환한다")
    @Test
    void signupFailWithEmailErrorWhenEmailAndNicknameDuplicated() throws Exception {
        signup("gildong", "gildong@example.com", "password123");

        SignupRequest duplicatedRequest = new SignupRequest(
            "gildong",
            "gildong@example.com",
            "password123"
        );

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicatedRequest)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("USER-002"));
    }

    @DisplayName("잘못된 요청값이면 회원가입에 실패한다")
    @Test
    void signupFailWhenRequestInvalid() throws Exception {
        SignupRequest invalidRequest = new SignupRequest(
                "",
                "invalid-email",
                "short"
        );

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-001"));
    }

    @DisplayName("이메일이 인증 도메인 정책을 위반하면 회원가입에 실패한다")
    @Test
    void signupFailWhenEmailViolatesCredentialPolicy() throws Exception {
        SignupRequest request = new SignupRequest(
                "gildong",
                "invalid-email",
                "password123"
        );

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH-013"));
    }

    @DisplayName("비밀번호가 인증 도메인 정책을 위반하면 회원가입에 실패한다")
    @Test
    void signupFailWhenPasswordViolatesCredentialPolicy() throws Exception {
        SignupRequest request = new SignupRequest(
                "gildong",
                "gildong@example.com",
                "password"
        );

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH-014"));
    }

    @DisplayName("이메일과 비밀번호가 올바르면 로그인에 성공한다")
    @Test
    void loginSuccess() throws Exception {
        SignupRequest signupRequest = new SignupRequest(
                "gildong",
                "gildong@example.com",
                "password123",
                "https://cdn.promsearch.com/profiles/me.png",
                List.of(),
                List.of()
        );
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated());

        LoginRequest request = new LoginRequest("gildong@example.com", "password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("COMMON-200"))
                .andExpect(jsonPath("$.result.accessToken", notNullValue()))
                .andExpect(jsonPath("$.result.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.result.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.result.expiresIn").value(3600))
                .andExpect(jsonPath("$.result.userId", notNullValue()))
                .andExpect(jsonPath("$.result.name").doesNotExist())
                .andExpect(jsonPath("$.result.profileImageUrl")
                        .value("https://cdn.promsearch.com/profiles/me.png"))
                .andExpect(jsonPath("$.result.nickname").value("gildong"))
                .andExpect(jsonPath("$.result.email").value("gildong@example.com"))
                .andExpect(jsonPath("$.result.password").doesNotExist());
    }

    @DisplayName("refresh token으로 access token 재발급에 성공한다")
    @Test
    void reissueSuccess() throws Exception {
        signup("gildong", "gildong@example.com", "password123");

        LoginRequest loginRequest = new LoginRequest("gildong@example.com", "password123");

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode loginResult = objectMapper.readTree(loginResponse).get("result");
        String refreshToken = loginResult.get("refreshToken").asText();

        ReissueRequest reissueRequest = new ReissueRequest(refreshToken);

        String reissueResponse = mockMvc.perform(post("/api/v1/auth/reissue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reissueRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("COMMON-200"))
                .andExpect(jsonPath("$.result.accessToken", notNullValue()))
                .andExpect(jsonPath("$.result.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.result.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.result.expiresIn").value(3600))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String rotatedRefreshToken = objectMapper.readTree(reissueResponse)
                .get("result")
                .get("refreshToken")
                .asText();
        assertThat(rotatedRefreshToken).isNotEqualTo(refreshToken);

        mockMvc.perform(post("/api/v1/auth/reissue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reissueRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH-004"));

        mockMvc.perform(post("/api/v1/auth/reissue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReissueRequest(rotatedRefreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH-004"));
    }

    @DisplayName("보호된 API는 access token 없이 접근할 수 없다")
    @Test
    void protectedApiRequiresAccessToken() throws Exception {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("새이름", null, null, null);

        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH-002"));
    }

    @DisplayName("보호된 API는 access token의 사용자 ID를 사용하고 X-User-Id 헤더를 신뢰하지 않는다")
    @Test
    void protectedApiUsesAuthenticatedPrincipal() throws Exception {
        signup("gildong", "gildong@example.com", "password123");
        String accessToken = loginAndGetResult("gildong@example.com", "password123")
                .get("accessToken")
                .asText();
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("새이름", null, null, null);

        mockMvc.perform(patch("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .header("X-User-Id", "999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result.name").value("새이름"))
                .andExpect(jsonPath("$.result.nickname").value("gildong"));
    }

    @DisplayName("프로필 이메일이 인증 도메인 정책을 위반하면 수정에 실패한다")
    @Test
    void updateProfileFailWhenEmailViolatesCredentialPolicy() throws Exception {
        signup("gildong", "gildong@example.com", "password123");
        String accessToken = loginAndGetResult("gildong@example.com", "password123")
                .get("accessToken")
                .asText();
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(null, null, "invalid-email", null);

        mockMvc.perform(patch("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH-013"));
    }

    @DisplayName("새 비밀번호가 인증 도메인 정책을 위반하면 변경에 실패한다")
    @Test
    void changePasswordFailWhenNewPasswordViolatesCredentialPolicy() throws Exception {
        signup("gildong", "gildong@example.com", "password123");
        String accessToken = loginAndGetResult("gildong@example.com", "password123")
                .get("accessToken")
                .asText();
        ChangePasswordRequest request = new ChangePasswordRequest("password123", "password");

        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH-014"));
    }

    @DisplayName("유효하지 않은 refresh token이면 access token 재발급에 실패한다")
    @Test
    void reissueFailWhenRefreshTokenInvalid() throws Exception {
        ReissueRequest request = new ReissueRequest("invalid-refresh-token");

        mockMvc.perform(post("/api/v1/auth/reissue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH-004"));
    }

    @DisplayName("존재하지 않는 이메일이면 로그인에 실패한다")
    @Test
    void loginFailWhenEmailNotFound() throws Exception {
        LoginRequest request = new LoginRequest("unknown@example.com", "password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH-001"));
    }

    @DisplayName("비밀번호가 일치하지 않으면 로그인에 실패한다")
    @Test
    void loginFailWhenPasswordMismatch() throws Exception {
        signup("gildong", "gildong@example.com", "password123");

        LoginRequest request = new LoginRequest("gildong@example.com", "wrong-password");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH-001"));
    }

    @DisplayName("ACTIVE 상태가 아닌 사용자는 로그인에 실패한다")
    @Test
    void loginFailWhenUserIsNotActive() throws Exception {
        signup("gildong", "gildong@example.com", "password123");
        jdbcTemplate.update("UPDATE users SET status = 'BANNED' WHERE email = ?", "gildong@example.com");
        entityManager.clear();

        LoginRequest request = new LoginRequest("gildong@example.com", "password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH-001"));
    }

    @DisplayName("잘못된 요청값이면 로그인에 실패한다")
    @Test
    void loginFailWhenRequestInvalid() throws Exception {
        LoginRequest request = new LoginRequest("invalid-email", "");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-001"));
    }

    @DisplayName("이메일이 인증 도메인 정책을 위반하면 로그인에 실패한다")
    @Test
    void loginFailWhenEmailViolatesCredentialPolicy() throws Exception {
        LoginRequest request = new LoginRequest("invalid-email", "password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH-013"));
    }


    @DisplayName("신규 카카오 계정으로 소셜 로그인하면 자동 회원가입 후 로그인에 성공한다")
    @Test
    void socialLoginSuccessWithNewKakaoAccount() throws Exception {
        given(kakaoOAuthAdapter.provider()).willReturn(SocialProvider.KAKAO);
        given(kakaoOAuthAdapter.exchangeCodeAndFetchUserInfo("auth-code", "https://promsearch.com/callback"))
                .willReturn(new SocialLoginResult(
                        "kakao-1",
                        "kakao-user@example.com",
                        "카카오유저",
                        "https://cdn.promsearch.com/profiles/kakao.png"
                ));

        SocialLoginRequest request = new SocialLoginRequest("auth-code", "https://promsearch.com/callback");

        mockMvc.perform(post("/api/v1/auth/oauth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("COMMON-200"))
                .andExpect(jsonPath("$.result.accessToken", notNullValue()))
                .andExpect(jsonPath("$.result.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.result.email").value("kakao-user@example.com"))
                .andExpect(jsonPath("$.result.nickname").value("카카오유저"))
                .andExpect(jsonPath("$.result.name").doesNotExist())
                .andExpect(jsonPath("$.result.profileImageUrl")
                        .value("https://cdn.promsearch.com/profiles/kakao.png"))
                .andExpect(jsonPath("$.result.password").doesNotExist());

        assertThat(userJpaRepository.existsByEmail("kakao-user@example.com")).isTrue();
    }

    @DisplayName("이미 연동된 카카오 계정으로 다시 로그인하면 회원을 새로 만들지 않는다")
    @Test
    void socialLoginReusesLinkedAccount() throws Exception {
        given(kakaoOAuthAdapter.provider()).willReturn(SocialProvider.KAKAO);
        given(kakaoOAuthAdapter.exchangeCodeAndFetchUserInfo("auth-code", "https://promsearch.com/callback"))
                .willReturn(new SocialLoginResult("kakao-2", "kakao-user2@example.com", "카카오유저2", null));

        SocialLoginRequest request = new SocialLoginRequest("auth-code", "https://promsearch.com/callback");

        String firstResponse = mockMvc.perform(post("/api/v1/auth/oauth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long firstUserId = objectMapper.readTree(firstResponse).get("result").get("userId").asLong();
        long userCountAfterFirstLogin = userJpaRepository.count();

        String secondResponse = mockMvc.perform(post("/api/v1/auth/oauth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long secondUserId = objectMapper.readTree(secondResponse).get("result").get("userId").asLong();

        assertThat(secondUserId).isEqualTo(firstUserId);
        assertThat(userJpaRepository.count()).isEqualTo(userCountAfterFirstLogin);
    }

    @DisplayName("신규 구글 계정으로 소셜 로그인하면 자동 회원가입 후 로그인에 성공한다")
    @Test
    void socialLoginSuccessWithNewGoogleAccount() throws Exception {
        given(googleOAuthAdapter.provider()).willReturn(SocialProvider.GOOGLE);
        given(googleOAuthAdapter.exchangeCodeAndFetchUserInfo("auth-code", "https://promsearch.com/callback"))
                .willReturn(new SocialLoginResult("google-1", "google-user@example.com", "구글유저", null));

        SocialLoginRequest request = new SocialLoginRequest("auth-code", "https://promsearch.com/callback");

        mockMvc.perform(post("/api/v1/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("COMMON-200"))
                .andExpect(jsonPath("$.result.accessToken", notNullValue()))
                .andExpect(jsonPath("$.result.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.result.email").value("google-user@example.com"))
                .andExpect(jsonPath("$.result.nickname").value("구글유저"))
                .andExpect(jsonPath("$.result.password").doesNotExist());

        assertThat(userJpaRepository.existsByEmail("google-user@example.com")).isTrue();
    }

    @DisplayName("이미 연동된 구글 계정으로 다시 로그인하면 회원을 새로 만들지 않는다")
    @Test
    void socialLoginReusesLinkedGoogleAccount() throws Exception {
        given(googleOAuthAdapter.provider()).willReturn(SocialProvider.GOOGLE);
        given(googleOAuthAdapter.exchangeCodeAndFetchUserInfo("auth-code", "https://promsearch.com/callback"))
                .willReturn(new SocialLoginResult("google-2", "google-user2@example.com", "구글유저2", null));

        SocialLoginRequest request = new SocialLoginRequest("auth-code", "https://promsearch.com/callback");

        String firstResponse = mockMvc.perform(post("/api/v1/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long firstUserId = objectMapper.readTree(firstResponse).get("result").get("userId").asLong();
        long userCountAfterFirstLogin = userJpaRepository.count();

        String secondResponse = mockMvc.perform(post("/api/v1/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long secondUserId = objectMapper.readTree(secondResponse).get("result").get("userId").asLong();

        assertThat(secondUserId).isEqualTo(firstUserId);
        assertThat(userJpaRepository.count()).isEqualTo(userCountAfterFirstLogin);
    }

    @DisplayName("지원하지 않는 소셜 로그인 제공자면 실패한다")
    @Test
    void socialLoginFailWhenProviderUnsupported() throws Exception {
        SocialLoginRequest request = new SocialLoginRequest("auth-code", "https://promsearch.com/callback");

        mockMvc.perform(post("/api/v1/auth/oauth/facebook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH-006"));
    }

    @DisplayName("소셜 계정에서 이메일을 가져올 수 없으면 소셜 로그인에 실패한다")
    @Test
    void socialLoginFailWhenEmailNotAvailable() throws Exception {
        given(kakaoOAuthAdapter.provider()).willReturn(SocialProvider.KAKAO);
        given(kakaoOAuthAdapter.exchangeCodeAndFetchUserInfo("auth-code", "https://promsearch.com/callback"))
                .willThrow(new AuthDomainException(AuthErrorCode.OAUTH_EMAIL_NOT_AVAILABLE));

        SocialLoginRequest request = new SocialLoginRequest("auth-code", "https://promsearch.com/callback");

        mockMvc.perform(post("/api/v1/auth/oauth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH-008"));
    }

    @DisplayName("소셜 제공자 서버 장애로 사용자 정보 조회에 실패하면 소셜 로그인에 실패한다")
    @Test
    void socialLoginFailWhenProviderUnavailable() throws Exception {
        given(kakaoOAuthAdapter.provider()).willReturn(SocialProvider.KAKAO);
        given(kakaoOAuthAdapter.exchangeCodeAndFetchUserInfo("auth-code", "https://promsearch.com/callback"))
                .willThrow(new AuthDomainException(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE));

        SocialLoginRequest request = new SocialLoginRequest("auth-code", "https://promsearch.com/callback");

        mockMvc.perform(post("/api/v1/auth/oauth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH-012"));
    }

    @DisplayName("인가 코드가 없으면 소셜 로그인 요청이 실패한다")
    @Test
    void socialLoginFailWhenRequestInvalid() throws Exception {
        SocialLoginRequest request = new SocialLoginRequest("", "");

        mockMvc.perform(post("/api/v1/auth/oauth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-001"));
    }

    private void signup(String nickname, String email, String password) throws Exception {
        SignupRequest request = new SignupRequest(nickname, email, password);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private JsonNode loginAndGetResult(String email, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(email, password);
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("result");
    }
}
