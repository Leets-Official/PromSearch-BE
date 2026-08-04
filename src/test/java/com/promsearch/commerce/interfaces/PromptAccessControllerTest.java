package com.promsearch.commerce.interfaces;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.promsearch.auth.application.usecase.AuthenticateAccessTokenUseCase;
import com.promsearch.commerce.application.usecase.CopyPromptUseCase;
import com.promsearch.commerce.application.usecase.UnlockPromptUseCase;
import com.promsearch.commerce.application.usecase.dto.CopyPromptCommand;
import com.promsearch.commerce.application.usecase.dto.CopyPromptInfo;
import com.promsearch.commerce.application.usecase.dto.UnlockPromptCommand;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PromptAccessController.class)
@AutoConfigureMockMvc(addFilters = false)
class PromptAccessControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AuthenticateAccessTokenUseCase authenticateAccessTokenUseCase;
    @MockitoBean UnlockPromptUseCase unlockPromptUseCase;
    @MockitoBean CopyPromptUseCase copyPromptUseCase;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unlocksPrompt() throws Exception {
        mockMvc.perform(post("/api/v1/prompts/{promptId}/unlock", 10L)
                        .with(request -> authenticated(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result").doesNotExist());

        verify(unlockPromptUseCase).unlock(new UnlockPromptCommand(1L, 10L));
    }

    @Test
    void returnsFullPromptBodyForCopy() throws Exception {
        when(copyPromptUseCase.copy(new CopyPromptCommand(1L, 10L)))
                .thenReturn(new CopyPromptInfo(10L, "full prompt body", true));

        mockMvc.perform(post("/api/v1/prompts/{promptId}/copy", 10L)
                        .with(request -> authenticated(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.promptId").value(10))
                .andExpect(jsonPath("$.result.promptBody").value("full prompt body"));

        verify(copyPromptUseCase).copy(new CopyPromptCommand(1L, 10L));
    }

    private org.springframework.mock.web.MockHttpServletRequest authenticated(
            org.springframework.mock.web.MockHttpServletRequest request
    ) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new AuthenticatedUserPrincipal(1L, "USER"),
                        null
                )
        );
        return request;
    }
}
