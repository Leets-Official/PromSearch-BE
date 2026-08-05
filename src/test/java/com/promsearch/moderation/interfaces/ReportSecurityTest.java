package com.promsearch.moderation.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ReportSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @DisplayName("게시글 신고는 인증 없이 접근할 수 없다")
    @Test
    void anonymousUserCannotReportPost() throws Exception {
        mockMvc.perform(post("/api/v1/reports/posts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"SPAM","description":"신고 설명"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("댓글 신고는 인증 없이 접근할 수 없다")
    @Test
    void anonymousUserCannotReportComment() throws Exception {
        mockMvc.perform(post("/api/v1/reports/comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"INAPPROPRIATE","description":"신고 설명"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
