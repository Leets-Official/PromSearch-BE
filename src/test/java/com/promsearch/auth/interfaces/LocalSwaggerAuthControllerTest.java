package com.promsearch.auth.interfaces;

import static org.hamcrest.Matchers.notNullValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promsearch.auth.interfaces.dto.request.SignupRequest;
import com.promsearch.auth.interfaces.dto.request.SwaggerTokenRequest;
import com.promsearch.user.interfaces.dto.request.UpdateUserProfileRequest;
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

        UpdateUserProfileRequest updateRequest = new UpdateUserProfileRequest("swagger-user", null, null);
        mockMvc.perform(patch("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").doesNotExist())
                .andExpect(jsonPath("$.result.nickname").value("swagger-user"));
    }

    private Long signupAndGetUserId() throws Exception {
        SignupRequest request = new SignupRequest(
                "gildong",
                "gildong@example.com",
                "password123"
        );

        String response = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode result = objectMapper.readTree(response).get("result");
        return result.get("userId").asLong();
    }
}
