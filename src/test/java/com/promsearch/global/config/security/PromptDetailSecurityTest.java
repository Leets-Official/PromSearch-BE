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
class PromptDetailSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @DisplayName("인증 없이 0 또는 음수 프롬프트 ID를 조회하면 유효성 오류를 반환한다")
    @Test
    void invalidPromptIdIsHandledByApplication() throws Exception {
        mockMvc.perform(get("/api/v1/prompts/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PROMPT-016"));

        mockMvc.perform(get("/api/v1/prompts/-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PROMPT-016"));
    }

    @DisplayName("인증 없이 존재하지 않는 프롬프트를 조회하면 찾을 수 없음 오류를 반환한다")
    @Test
    void missingPromptIsHandledByApplication() throws Exception {
        mockMvc.perform(get("/api/v1/prompts/9223372036854775807"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMPT-001"));
    }

    @DisplayName("숫자가 아닌 프롬프트 ID도 인증 오류 대신 잘못된 요청으로 처리한다")
    @Test
    void nonNumericPromptIdIsHandledByMvc() throws Exception {
        mockMvc.perform(get("/api/v1/prompts/not-a-number"))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("인증이 필요한 임시저장 조회 API는 공개하지 않는다")
    @Test
    void draftPromptRemainsProtected() throws Exception {
        mockMvc.perform(get("/api/v1/prompts/draft"))
                .andExpect(status().isUnauthorized());
    }
}
