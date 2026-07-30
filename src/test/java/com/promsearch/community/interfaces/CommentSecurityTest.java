package com.promsearch.community.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommentSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @DisplayName("댓글 목록 조회는 인증 없이 컨트롤러까지 접근할 수 있다")
    @Test
    void anonymousUserCanAccessCommentList() throws Exception {
        mockMvc.perform(get("/api/v1/prompts/999999/comments"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMUNITY-015"));
    }

    @DisplayName("커서와 조회 크기가 있어도 댓글 목록은 인증 없이 접근할 수 있다")
    @Test
    void anonymousUserCanAccessPaginatedCommentList() throws Exception {
        mockMvc.perform(get("/api/v1/prompts/999999/comments")
                        .param("cursor", "1")
                        .param("size", "2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMUNITY-015"));
    }

    @DisplayName("0 또는 음수 프롬프트 ID는 인증 오류 대신 유효성 오류로 처리한다")
    @Test
    void invalidPromptIdIsHandledByApplication() throws Exception {
        mockMvc.perform(get("/api/v1/prompts/0/comments"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMUNITY-011"));

        mockMvc.perform(get("/api/v1/prompts/-1/comments"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMUNITY-011"));
    }

    @DisplayName("숫자가 아닌 프롬프트 ID는 인증 오류 대신 잘못된 요청으로 처리한다")
    @Test
    void nonNumericPromptIdIsHandledByMvc() throws Exception {
        mockMvc.perform(get("/api/v1/prompts/not-a-number/comments"))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("대댓글 목록도 인증 없이 애플리케이션까지 접근할 수 있다")
    @Test
    void anonymousUserCanAccessReplyList() throws Exception {
        mockMvc.perform(get("/api/v1/comments/999999/replies"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMUNITY-001"));
    }

    @DisplayName("커서와 조회 크기가 있어도 답글 목록은 인증 없이 접근할 수 있다")
    @Test
    void anonymousUserCanAccessPaginatedReplyList() throws Exception {
        mockMvc.perform(get("/api/v1/comments/999999/replies")
                        .param("cursor", "1")
                        .param("size", "2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMUNITY-001"));
    }

    @DisplayName("댓글 작성은 인증 없이 접근할 수 없다")
    @Test
    void anonymousUserCannotCreateComment() throws Exception {
        mockMvc.perform(post("/api/v1/prompts/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"댓글"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
