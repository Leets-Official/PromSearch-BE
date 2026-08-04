package com.promsearch.prompt.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.promsearch.auth.application.usecase.AuthenticateAccessTokenUseCase;
import com.promsearch.prompt.application.usecase.ListHomePromptsUseCase;
import com.promsearch.prompt.application.usecase.dto.HomePromptListQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HomeController.class)
@AutoConfigureMockMvc(addFilters = false)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticateAccessTokenUseCase authenticateAccessTokenUseCase;

    @MockitoBean
    private ListHomePromptsUseCase listHomePromptsUseCase;

    @DisplayName("인기 프롬프트 목록은 offset overflow가 가능한 page를 400으로 거절한다")
    @Test
    void listPopularPromptsRejectsPageBeyondOffsetBoundary() throws Exception {
        mockMvc.perform(get("/api/v1/home/prompts/popular")
                        .param("page", String.valueOf(HomePromptListQuery.MAX_PAGE + 1))
                        .param("size", String.valueOf(HomePromptListQuery.MAX_SIZE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-400"));
    }
}
