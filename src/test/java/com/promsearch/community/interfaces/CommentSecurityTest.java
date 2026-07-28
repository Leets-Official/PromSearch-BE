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
