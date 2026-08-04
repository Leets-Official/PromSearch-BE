package com.promsearch.prompt.interfaces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.promsearch.auth.application.usecase.AuthenticateAccessTokenUseCase;
import com.promsearch.prompt.application.usecase.ListHomePromptsUseCase;
import com.promsearch.prompt.application.usecase.dto.HomePromptListInfo;
import com.promsearch.prompt.application.usecase.dto.HomePromptListQuery;
import com.promsearch.prompt.application.usecase.dto.HomePromptSort;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

    @DisplayName("홈 프롬프트 목록은 직군, 태스크, 모델, 결과물, 검색어 필터를 조합해 조회한다")
    @Test
    void listPromptsDelegatesCombinedFilters() throws Exception {
        when(listHomePromptsUseCase.listPrompts(any()))
                .thenReturn(new HomePromptListInfo(List.of(), 0, 12, 0, false));

        mockMvc.perform(get("/api/v1/home/prompts")
                        .param("jobTagId", "1")
                        .param("taskTagIds", "2", "3")
                        .param("aiModelTagId", "4")
                        .param("outputType", "TEXT")
                        .param("keyword", "보고서")
                        .param("sort", "POPULAR")
                        .param("page", "0")
                        .param("size", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        ArgumentCaptor<HomePromptListQuery> queryCaptor = ArgumentCaptor.forClass(HomePromptListQuery.class);
        verify(listHomePromptsUseCase).listPrompts(queryCaptor.capture());
        HomePromptListQuery query = queryCaptor.getValue();
        assertThat(query.jobTagId()).isEqualTo(1L);
        assertThat(query.taskTagIds()).containsExactly(2L, 3L);
        assertThat(query.aiModelTagId()).isEqualTo(4L);
        assertThat(query.outputType()).isEqualTo(PromptOutputType.TEXT);
        assertThat(query.keyword()).isEqualTo("보고서");
        assertThat(query.sort()).isEqualTo(HomePromptSort.POPULAR);
    }

    @DisplayName("홈 프롬프트 목록은 허용 개수를 넘는 태스크 필터를 400으로 거절한다")
    @Test
    void listPromptsRejectsTooManyTaskFilters() throws Exception {
        mockMvc.perform(get("/api/v1/home/prompts")
                        .param("taskTagIds", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-400"));
    }

    @DisplayName("홈 프롬프트 목록은 trim 후 두 글자 미만인 검색어를 400으로 거절한다")
    @Test
    void listPromptsRejectsShortKeyword() throws Exception {
        mockMvc.perform(get("/api/v1/home/prompts")
                        .param("keyword", " a "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-400"));
    }

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
