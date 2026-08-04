package com.promsearch.moderation.interfaces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.promsearch.auth.application.usecase.AuthenticateAccessTokenUseCase;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import com.promsearch.moderation.application.usecase.CreateCommentReportUseCase;
import com.promsearch.moderation.application.usecase.CreatePostReportUseCase;
import com.promsearch.moderation.application.usecase.dto.CreatePostReportCommand;
import com.promsearch.moderation.domain.enums.ReportReason;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReportControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AuthenticateAccessTokenUseCase authenticateAccessTokenUseCase;
    @MockitoBean CreatePostReportUseCase createPostReportUseCase;
    @MockitoBean CreateCommentReportUseCase createCommentReportUseCase;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsPostReport() throws Exception {
        mockMvc.perform(post("/api/v1/reports/posts/{postId}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"SPAM\",\"description\":\"spam\"}")
                        .with(request -> {
                            SecurityContextHolder.getContext().setAuthentication(authentication());
                            return request;
                        }))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("COMMON-201"))
                .andExpect(jsonPath("$.message").value("신고가 접수되었습니다."))
                .andExpect(jsonPath("$.result").doesNotExist());

        ArgumentCaptor<CreatePostReportCommand> captor =
                ArgumentCaptor.forClass(CreatePostReportCommand.class);
        Mockito.verify(createPostReportUseCase).create(captor.capture());
        assertThat(captor.getValue()).isEqualTo(
                new CreatePostReportCommand(1L, 10L, ReportReason.SPAM, "spam"));
    }

    @Test
    void rejectsBlankDescription() throws Exception {
        mockMvc.perform(post("/api/v1/reports/comments/{commentId}", 20L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"SPAM\",\"description\":\" \"}")
                        .with(request -> {
                            SecurityContextHolder.getContext().setAuthentication(authentication());
                            return request;
                        }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-001"));
    }

    private UsernamePasswordAuthenticationToken authentication() {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUserPrincipal(1L, "USER"), null);
    }
}
