package com.promsearch.prompt.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.promsearch.auth.application.port.out.token.AccessTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PromptController.class)
@AutoConfigureMockMvc(addFilters = false)
class PromptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccessTokenProvider accessTokenProvider;

    @DisplayName("프롬프트 인터페이스 6개는 가짜 성공 대신 구현 중 응답을 반환한다")
    @Test
    void promptInterfacesReturnNotImplemented() throws Exception {
        expectNotImplemented(post("/api/v1/prompt-images/upload-urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validUploadUrlRequest()));

        expectNotImplemented(put("/api/v1/prompts/draft")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validDraftRequest()));

        expectNotImplemented(get("/api/v1/prompts/draft"));
        expectNotImplemented(delete("/api/v1/prompts/draft"));

        expectNotImplemented(post("/api/v1/prompts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validCreateRequest()));

        expectNotImplemented(delete("/api/v1/prompts/1"));
    }

    @DisplayName("임시저장은 제목이 공백이거나 20자를 초과하면 거절한다")
    @Test
    void draftTitleValidation() throws Exception {
        expectBadRequest(put("/api/v1/prompts/draft")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"   "}
                        """));

        expectBadRequest(put("/api/v1/prompts/draft")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"123456789012345678901"}
                        """));
    }

    @DisplayName("MASTER 콘텐츠 타입은 생성 요청에서 허용하지 않는다")
    @Test
    void masterContentTypeIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/prompts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"회의록 자동 정리",
                                  "outputType":"TEXT",
                                  "contentType":"MASTER",
                                  "promptBody":"회의록을 정리해 주세요."
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-002"));
    }

    @DisplayName("프롬프트 이미지에서 썸네일은 최대 한 장만 지정할 수 있다")
    @Test
    void onlyOneThumbnailIsAllowed() throws Exception {
        expectBadRequest(post("/api/v1/prompts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title":"회의록 자동 정리",
                          "outputType":"IMAGE",
                          "contentType":"FREE",
                          "promptBody":"이미지를 생성해 주세요.",
                          "images":[
                            {"imageId":"123e4567-e89b-12d3-a456-426614174000","sortOrder":0,"thumbnail":true},
                            {"imageId":"123e4567-e89b-12d3-a456-426614174001","sortOrder":1,"thumbnail":true}
                          ]
                        }
                        """));
    }

    @DisplayName("업로드 URL 요청은 허용 형식과 이미지 크기 정책을 검증한다")
    @Test
    void imageUploadMetadataValidation() throws Exception {
        expectBadRequest(post("/api/v1/prompt-images/upload-urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "images":[{
                            "fileName":"result.gif",
                            "contentType":"image/gif",
                            "fileSize":10485761,
                            "width":8192,
                            "height":8192
                          }]
                        }
                        """));
    }

    private void expectNotImplemented(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-501"));
    }

    private void expectBadRequest(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-001"));
    }

    private String validUploadUrlRequest() {
        return """
                {
                  "images":[{
                    "fileName":"prompt-result.webp",
                    "contentType":"image/webp",
                    "fileSize":5242880,
                    "width":1920,
                    "height":1080
                  }]
                }
                """;
    }

    private String validDraftRequest() {
        return """
                {"title":"회의록 자동 정리"}
                """;
    }

    private String validCreateRequest() {
        return """
                {
                  "title":"회의록 자동 정리",
                  "description":"긴 회의록에서 핵심 내용을 정리합니다.",
                  "outputType":"TEXT",
                  "jobTagIds":[1,2],
                  "taskTagIds":[10],
                  "aiModelTagIds":[20],
                  "customAiModel":"GPT 4.1 Mini",
                  "contentType":"FREE",
                  "promptBody":"회의록을 읽고 결정 사항과 할 일을 정리해 주세요.",
                  "images":[]
                }
                """;
    }
}
