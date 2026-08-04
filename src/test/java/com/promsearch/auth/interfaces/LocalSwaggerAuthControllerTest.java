package com.promsearch.auth.interfaces;

import static org.hamcrest.Matchers.notNullValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promsearch.auth.interfaces.dto.request.SignupRequest;
import com.promsearch.auth.interfaces.dto.request.SwaggerTokenRequest;
import com.promsearch.user.interfaces.dto.request.UpdateUserProfileRequest;
import com.promsearch.user.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@TestPropertySource(properties = "SWAGGER_ENABLE=true")
@Transactional
class LocalSwaggerAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @DisplayName("회원가입 Swagger 요청 스키마는 이름을 노출하지 않는다")
    @Test
    void signupSchemaDoesNotExposeName() throws Exception {
        String response = mockMvc.perform(get("/docs-json"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var properties = objectMapper.readTree(response)
                .at("/components/schemas/SignupRequest/properties");

        assertThat(properties.has("name")).isFalse();
        assertThat(properties.has("nickname")).isTrue();
        assertThat(properties.has("email")).isTrue();
        assertThat(properties.has("password")).isTrue();
        assertThat(properties.has("profileImageUrl")).isTrue();
        assertThat(properties.has("jobTags")).isTrue();
        assertThat(properties.has("taskTags")).isTrue();
    }

    @DisplayName("로그인 응답 Swagger 스키마는 이름 대신 프로필 이미지 URL을 노출한다")
    @Test
    void loginResponseSchemaExposesProfileImageInsteadOfName() throws Exception {
        String response = mockMvc.perform(get("/docs-json"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var properties = objectMapper.readTree(response)
                .at("/components/schemas/LoginResponse/properties");

        assertThat(properties.has("name")).isFalse();
        assertThat(properties.has("profileImageUrl")).isTrue();
        assertThat(properties.has("nickname")).isTrue();
        assertThat(properties.has("email")).isTrue();
    }

    @DisplayName("local Swagger token API로 받은 Authorization 헤더값으로 보호 API를 호출할 수 있다")
    @Test
    void swaggerTokenCanAuthorizeProtectedApiInLocal() throws Exception {
        Long userId = signupAndGetUserId();
        SwaggerTokenRequest tokenRequest = new SwaggerTokenRequest(userId, "gildong@example.com", "USER");

        String tokenResponse = mockMvc.perform(post("/api/v1/auth/swagger-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tokenRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result.accessToken", notNullValue()))
                .andExpect(jsonPath("$.result.authorizationHeader", notNullValue()))
                .andExpect(jsonPath("$.result.userId").value(userId))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String authorizationHeader = objectMapper.readTree(tokenResponse)
                .get("result")
                .get("authorizationHeader")
                .asText();
        assertThat(authorizationHeader).startsWith("Bearer ");

        UpdateUserProfileRequest updateRequest = new UpdateUserProfileRequest("스웨거", null, null);
        mockMvc.perform(patch("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("스웨거"));
    }

    private Long signupAndGetUserId() throws Exception {
        SignupRequest request = new SignupRequest(
                "gildong",
                "gildong@example.com",
                "password123"
        );

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        return userRepository.findByEmail("gildong@example.com")
                .orElseThrow()
                .toDomain()
                .getUserId()
                .id();
    }
}
