package com.promsearch.community.interfaces;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.promsearch.auth.application.usecase.AuthenticateAccessTokenUseCase;
import com.promsearch.community.application.usecase.CreateCommentReplyUseCase;
import com.promsearch.community.application.usecase.CreateCommentUseCase;
import com.promsearch.community.application.usecase.DeleteCommentUseCase;
import com.promsearch.community.application.usecase.GetCommentsUseCase;
import com.promsearch.community.application.usecase.UpdateCommentUseCase;
import com.promsearch.community.application.usecase.dto.CommentAuthorInfo;
import com.promsearch.community.application.usecase.dto.CommentInfo;
import com.promsearch.community.application.usecase.dto.CommentListInfo;
import com.promsearch.community.application.usecase.dto.CommentReplyInfo;
import com.promsearch.community.domain.enums.CommentStatus;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
class CommentControllerTest {

    private static final Instant NOW = Instant.parse("2026-07-28T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticateAccessTokenUseCase authenticateAccessTokenUseCase;
    @MockitoBean
    private GetCommentsUseCase getCommentsUseCase;
    @MockitoBean
    private CreateCommentUseCase createCommentUseCase;
    @MockitoBean
    private UpdateCommentUseCase updateCommentUseCase;
    @MockitoBean
    private DeleteCommentUseCase deleteCommentUseCase;
    @MockitoBean
    private CreateCommentReplyUseCase createCommentReplyUseCase;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @DisplayName("비로그인 사용자도 댓글 목록을 조회할 수 있다")
    @Test
    void getCommentsWithoutAuthentication() throws Exception {
        given(getCommentsUseCase.getComments(any()))
                .willReturn(new CommentListInfo(List.of(commentInfo())));

        mockMvc.perform(get("/api/v1/prompts/10/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON-200"))
                .andExpect(jsonPath("$.result.comments[0].commentId").value(100))
                .andExpect(jsonPath("$.result.comments[0].mine").value(true))
                .andExpect(jsonPath("$.result.comments[0].replies[0].commentId").value(101));
    }

    @DisplayName("댓글을 생성하면 201 응답을 반환한다")
    @Test
    void createComment() throws Exception {
        authenticate();
        given(createCommentUseCase.createComment(any())).willReturn(commentInfo());

        mockMvc.perform(post("/api/v1/prompts/10/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"좋은 프롬프트네요."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("COMMON-201"))
                .andExpect(jsonPath("$.result.commentId").value(100));
    }

    @DisplayName("빈 댓글 내용은 요청 검증에서 거부한다")
    @Test
    void createCommentRejectsBlankContent() throws Exception {
        authenticate();
        mockMvc.perform(post("/api/v1/prompts/10/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @DisplayName("댓글 수정과 삭제 API가 유스케이스 결과를 반환한다")
    @Test
    void updateAndDeleteComment() throws Exception {
        authenticate();
        given(updateCommentUseCase.updateComment(any())).willReturn(commentInfo());

        mockMvc.perform(patch("/api/v1/comments/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"수정한 댓글"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.commentId").value(100));

        mockMvc.perform(delete("/api/v1/comments/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON-200"));
    }

    @DisplayName("답글을 생성하면 201 응답을 반환한다")
    @Test
    void createReply() throws Exception {
        authenticate();
        given(createCommentReplyUseCase.createReply(any())).willReturn(replyInfo());

        mockMvc.perform(post("/api/v1/comments/100/replies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"답글입니다."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("COMMON-201"))
                .andExpect(jsonPath("$.result.parentCommentId").value(100));
    }

    private void authenticate() {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(2L, "USER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }

    private CommentInfo commentInfo() {
        return new CommentInfo(
                100L,
                null,
                new CommentAuthorInfo(2L, "author", "profile.png"),
                "댓글",
                CommentStatus.ACTIVE,
                true,
                false,
                NOW,
                NOW,
                List.of(replyInfo())
        );
    }

    private CommentReplyInfo replyInfo() {
        return new CommentReplyInfo(
                101L,
                100L,
                new CommentAuthorInfo(1L, "prompt-owner", null),
                "답글",
                CommentStatus.ACTIVE,
                false,
                true,
                NOW,
                NOW
        );
    }
}
