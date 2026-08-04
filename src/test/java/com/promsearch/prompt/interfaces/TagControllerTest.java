package com.promsearch.prompt.interfaces;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.promsearch.auth.application.usecase.AuthenticateAccessTokenUseCase;
import com.promsearch.prompt.application.usecase.ListPromptTagsUseCase;
import com.promsearch.prompt.application.usecase.dto.PromptTagInfo;
import com.promsearch.prompt.domain.enums.TagType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TagController.class)
@AutoConfigureMockMvc(addFilters = false)
class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticateAccessTokenUseCase authenticateAccessTokenUseCase;

    @MockitoBean
    private ListPromptTagsUseCase listPromptTagsUseCase;

    @DisplayName("tagType을 지정하면 해당 타입의 기존 태그만 조회한다")
    @Test
    void listTags() throws Exception {
        when(listPromptTagsUseCase.listByType(TagType.JOB))
                .thenReturn(List.of(
                        new PromptTagInfo(1L, TagType.JOB, "학생"),
                        new PromptTagInfo(2L, TagType.JOB, "직장인")
                ));

        mockMvc.perform(get("/api/v1/tags").param("tagType", "JOB"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result.tags[0].tagId").value(1))
                .andExpect(jsonPath("$.result.tags[0].tagType").value("JOB"))
                .andExpect(jsonPath("$.result.tags[0].name").value("학생"))
                .andExpect(jsonPath("$.result.tags[1].name").value("직장인"));
    }

    @DisplayName("tagType을 생략하면 전체 태그를 조회한다")
    @Test
    void listAllTags() throws Exception {
        when(listPromptTagsUseCase.listByType(null))
                .thenReturn(List.of(
                        new PromptTagInfo(1L, TagType.JOB, "학생"),
                        new PromptTagInfo(10L, TagType.TASK, "PPT"),
                        new PromptTagInfo(20L, TagType.AI_MODEL, "ChatGPT")
                ));

        mockMvc.perform(get("/api/v1/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.tags[0].tagType").value("JOB"))
                .andExpect(jsonPath("$.result.tags[1].tagType").value("TASK"))
                .andExpect(jsonPath("$.result.tags[2].tagType").value("AI_MODEL"));
    }

    @DisplayName("태그 목록은 지원하지 않는 타입 문자열을 400으로 거절한다")
    @Test
    void listTagsRejectsInvalidType() throws Exception {
        mockMvc.perform(get("/api/v1/tags").param("tagType", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-001"));
    }
}
