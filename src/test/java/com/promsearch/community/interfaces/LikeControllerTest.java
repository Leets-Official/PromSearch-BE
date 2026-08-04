package com.promsearch.community.interfaces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.promsearch.auth.application.usecase.AuthenticateAccessTokenUseCase;
import com.promsearch.community.application.usecase.LikePromptUseCase;
import com.promsearch.community.application.usecase.UnlikePromptUseCase;
import com.promsearch.community.application.usecase.dto.LikeInfo;
import com.promsearch.community.application.usecase.dto.LikePromptCommand;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LikeController.class)
@AutoConfigureMockMvc(addFilters = false)
class LikeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticateAccessTokenUseCase authenticateAccessTokenUseCase;

    @MockitoBean
    private LikePromptUseCase likePromptUseCase;

    @MockitoBean
    private UnlikePromptUseCase unlikePromptUseCase;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @DisplayName("인증 사용자의 좋아요를 등록하고 201 응답을 반환한다")
    @Test
    void like() throws Exception {
        Mockito.when(likePromptUseCase.like(Mockito.any()))
                .thenReturn(new LikeInfo(10L, true, 3L));

        mockMvc.perform(post("/api/v1/prompts/{promptId}/likes", 10L)
                        .with(request -> {
                            SecurityContextHolder.getContext().setAuthentication(authenticationPrincipal());
                            return request;
                        }))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("COMMON-201"))
                .andExpect(jsonPath("$.result.promptId").value(10))
                .andExpect(jsonPath("$.result.liked").value(true))
                .andExpect(jsonPath("$.result.likeCount").value(3));

        ArgumentCaptor<LikePromptCommand> captor = ArgumentCaptor.forClass(LikePromptCommand.class);
        Mockito.verify(likePromptUseCase).like(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new LikePromptCommand(1L, 10L));
    }

    @DisplayName("인증 사용자의 좋아요를 취소하고 동일한 응답 DTO로 현재 상태를 반환한다")
    @Test
    void unlike() throws Exception {
        Mockito.when(unlikePromptUseCase.unlike(Mockito.any()))
                .thenReturn(new LikeInfo(10L, false, 2L));

        mockMvc.perform(delete("/api/v1/prompts/{promptId}/likes", 10L)
                        .with(request -> {
                            SecurityContextHolder.getContext().setAuthentication(authenticationPrincipal());
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result.promptId").value(10))
                .andExpect(jsonPath("$.result.liked").value(false))
                .andExpect(jsonPath("$.result.likeCount").value(2));
    }

    @DisplayName("프롬프트 식별자는 양수여야 한다")
    @Test
    void promptIdMustBePositive() throws Exception {
        mockMvc.perform(post("/api/v1/prompts/{promptId}/likes", 0L)
                        .with(request -> {
                            SecurityContextHolder.getContext().setAuthentication(authenticationPrincipal());
                            return request;
                        }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-400"));
    }

    private UsernamePasswordAuthenticationToken authenticationPrincipal() {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUserPrincipal(1L, "USER"),
                null
        );
    }
}
