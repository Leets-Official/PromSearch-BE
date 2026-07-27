package com.promsearch.global.config.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SwaggerDisabledMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @DisplayName("기본 설정에서는 OpenAPI JSON이 노출되지 않는다")
    @Test
    void docsJsonDisabledByDefault() throws Exception {
        mockMvc.perform(get("/docs-json"))
                .andExpect(status().isNotFound());
    }

    @DisplayName("Spring Security 추가 후에도 일반 헬스체크 API는 Basic Auth 없이 접근 가능하다")
    @Test
    void ordinaryApiIsNotGloballyProtectedByBasicAuth() throws Exception {
        mockMvc.perform(get("/test/health-check"))
                .andExpect(status().isOk());
    }

    @DisplayName("닉네임 중복 확인 API는 회원가입 전에 인증 없이 호출할 수 있다")
    @Test
    void nicknameAvailabilityIsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/users/nicknames/availability")
                        .param("nickname", "new-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.available").value(true));
    }
}
