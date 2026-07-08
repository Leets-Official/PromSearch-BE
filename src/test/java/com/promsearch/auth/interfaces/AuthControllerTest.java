package com.promsearch.auth.interfaces;

import static org.hamcrest.Matchers.notNullValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promsearch.auth.interfaces.dto.SignupRequest;
import com.promsearch.user.infrastructure.persistence.UserJpaEntity;
import com.promsearch.user.infrastructure.persistence.UserJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private UserJpaRepository userJpaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @DisplayName("회원가입에 성공한다")
    @Test
    void signupSuccess() throws Exception {
        SignupRequest request = new SignupRequest(
                "홍길동",
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
                .andExpect(jsonPath("$.result.userId", notNullValue()))
                .andExpect(jsonPath("$.result.name").value("홍길동"))
                .andExpect(jsonPath("$.result.nickname").value("gildong"))
                .andExpect(jsonPath("$.result.email").value("gildong@example.com"))
                .andExpect(jsonPath("$.result.password").doesNotExist());

        UserJpaEntity savedUser = userJpaRepository.findByEmail("gildong@example.com").orElseThrow();
        assertThat(savedUser.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", savedUser.getPassword())).isTrue();
    }

    @DisplayName("이미 사용 중인 닉네임이면 회원가입에 실패한다")
    @Test
    void signupFailWhenNicknameDuplicated() throws Exception {
        signup("홍길동", "gildong", "gildong@example.com", "password123");

        SignupRequest duplicatedNicknameRequest = new SignupRequest(
                "김길동",
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
        signup("홍길동", "gildong", "gildong@example.com", "password123");

        SignupRequest duplicatedEmailRequest = new SignupRequest(
                "김길동",
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

    @DisplayName("잘못된 요청값이면 회원가입에 실패한다")
    @Test
    void signupFailWhenRequestInvalid() throws Exception {
        SignupRequest invalidRequest = new SignupRequest(
                "",
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

    private void signup(String name, String nickname, String email, String password) throws Exception {
        SignupRequest request = new SignupRequest(name, nickname, email, password);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
