package com.promsearch.prompt.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.promsearch.auth.application.usecase.AuthenticateAccessTokenUseCase;
import com.promsearch.global.config.JacksonConfig;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import com.promsearch.prompt.application.usecase.CompletePromptImageUploadUseCase;
import com.promsearch.prompt.application.usecase.CreatePromptUseCase;
import com.promsearch.prompt.application.usecase.GetPromptImageStatusesUseCase;
import com.promsearch.prompt.application.usecase.IssuePromptImageUploadUrlsUseCase;
import com.promsearch.prompt.application.usecase.dto.CreatePromptCommand;
import com.promsearch.prompt.application.usecase.dto.PromptCommandInfo;
import com.promsearch.prompt.application.usecase.dto.PromptImageStatusInfo;
import com.promsearch.prompt.application.usecase.dto.PromptImageStatusesInfo;
import com.promsearch.prompt.application.usecase.dto.PromptImageUploadInfo;
import com.promsearch.prompt.application.usecase.dto.PromptImageUploadUrlsInfo;
import com.promsearch.prompt.domain.Prompt;
import com.promsearch.prompt.domain.enums.PromptImageStatus;
import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.domain.enums.PromptVisibility;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PromptController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(JacksonConfig.class)
class PromptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticateAccessTokenUseCase authenticateAccessTokenUseCase;

    @MockitoBean
    private IssuePromptImageUploadUrlsUseCase issuePromptImageUploadUrlsUseCase;

    @MockitoBean
    private CompletePromptImageUploadUseCase completePromptImageUploadUseCase;

    @MockitoBean
    private CreatePromptUseCase createPromptUseCase;

    @MockitoBean
    private GetPromptImageStatusesUseCase getPromptImageStatusesUseCase;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @DisplayName("아직 구현하지 않은 프롬프트 인터페이스 4개는 구현 중 응답을 반환한다")
    @Test
    void promptInterfacesReturnNotImplemented() throws Exception {
        expectNotImplemented(put("/api/v1/prompts/draft")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validDraftRequest()));

        expectNotImplemented(get("/api/v1/prompts/draft"));
        expectNotImplemented(delete("/api/v1/prompts/draft"));

        expectNotImplemented(delete("/api/v1/prompts/1"));
    }

    @DisplayName("인증 사용자의 프롬프트를 생성하고 201 응답을 반환한다")
    @Test
    void createPrompt() throws Exception {
        Instant updatedAt = Instant.parse("2026-07-28T12:00:00Z");
        Mockito.when(createPromptUseCase.create(Mockito.any()))
                .thenReturn(new PromptCommandInfo(
                        10L,
                        PromptStatus.ACTIVE,
                        PromptVisibility.PUBLIC,
                        0L,
                        updatedAt
                ));

        mockMvc.perform(post("/api/v1/prompts")
                        .with(request -> {
                            SecurityContextHolder.getContext().setAuthentication(authenticationPrincipal());
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest().replace(
                                "회의록 자동 정리",
                                "가".repeat(Prompt.MAX_TITLE_LENGTH)
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("COMMON-201"))
                .andExpect(jsonPath("$.result.promptId").value(10))
                .andExpect(jsonPath("$.result.status").value("ACTIVE"))
                .andExpect(jsonPath("$.result.visibility").value("PUBLIC"))
                .andExpect(jsonPath("$.result.pricePoint").value(0))
                .andExpect(jsonPath("$.result.updatedAt").value("2026-07-28T21:00:00+09:00"))
                .andExpect(jsonPath("$.result.promptBody").doesNotExist());

        ArgumentCaptor<CreatePromptCommand> captor = ArgumentCaptor.forClass(CreatePromptCommand.class);
        Mockito.verify(createPromptUseCase).create(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().userId()).isEqualTo(1L);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().title())
                .hasSize(Prompt.MAX_TITLE_LENGTH);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().visibility())
                .isEqualTo(PromptVisibility.PUBLIC);
    }

    @DisplayName("인증 사용자의 이미지 업로드 URL을 발급한다")
    @Test
    void issueImageUploadUrls() throws Exception {
        UUID imageId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        Mockito.when(issuePromptImageUploadUrlsUseCase.issue(Mockito.any()))
                .thenReturn(new PromptImageUploadUrlsInfo(List.of(
                        new PromptImageUploadUrlsInfo.UploadTarget(
                                imageId,
                                URI.create("https://s3.example.com/upload"),
                                Instant.parse("2026-07-26T01:10:00Z")
                        )
                )));

        mockMvc.perform(post("/api/v1/prompt-images/upload-urls")
                        .with(request -> {
                            SecurityContextHolder.getContext().setAuthentication(authenticationPrincipal());
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUploadUrlRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result.images[0].imageId").value(imageId.toString()))
                .andExpect(jsonPath("$.result.images[0].uploadUrl").value("https://s3.example.com/upload"));
    }

    @DisplayName("S3 이미지 업로드 완료를 확인한다")
    @Test
    void completeImageUpload() throws Exception {
        UUID imageId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        Mockito.when(completePromptImageUploadUseCase.complete(Mockito.any()))
                .thenReturn(new PromptImageUploadInfo(
                        imageId,
                        PromptImageStatus.UPLOADED,
                        Instant.parse("2026-07-26T01:00:00Z")
                ));

        mockMvc.perform(post("/api/v1/prompt-images/{imageId}/complete", imageId)
                        .with(request -> {
                            SecurityContextHolder.getContext().setAuthentication(authenticationPrincipal());
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result.imageId").value(imageId.toString()))
                .andExpect(jsonPath("$.result.status").value("UPLOADED"));
    }

    @DisplayName("인증 사용자의 이미지 처리 상태를 요청 순서대로 조회한다")
    @Test
    void getImageStatuses() throws Exception {
        UUID firstImageId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID secondImageId = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
        Mockito.when(getPromptImageStatusesUseCase.getStatuses(Mockito.any()))
                .thenReturn(new PromptImageStatusesInfo(List.of(
                        new PromptImageStatusInfo(firstImageId, PromptImageStatus.PROCESSING, null),
                        new PromptImageStatusInfo(secondImageId, PromptImageStatus.FAILED, "WATERMARK_RENDER_FAILED")
                )));

        mockMvc.perform(get("/api/v1/prompt-images/statuses")
                        .param("imageIds", firstImageId + "," + secondImageId)
                        .with(request -> {
                            SecurityContextHolder.getContext().setAuthentication(authenticationPrincipal());
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result.images[0].imageId").value(firstImageId.toString()))
                .andExpect(jsonPath("$.result.images[0].status").value("PROCESSING"))
                .andExpect(jsonPath("$.result.images[0].failureCode").doesNotExist())
                .andExpect(jsonPath("$.result.images[1].imageId").value(secondImageId.toString()))
                .andExpect(jsonPath("$.result.images[1].status").value("FAILED"))
                .andExpect(jsonPath("$.result.images[1].failureCode").value("WATERMARK_RENDER_FAILED"));
    }

    @DisplayName("이미지 상태 조회는 최대 10개까지만 허용한다")
    @Test
    void imageStatusQueryAcceptsUpToTenImageIds() throws Exception {
        String imageIds = java.util.stream.Stream.generate(UUID::randomUUID)
                .limit(11)
                .map(UUID::toString)
                .collect(java.util.stream.Collectors.joining(","));

        mockMvc.perform(get("/api/v1/prompt-images/statuses")
                        .param("imageIds", imageIds)
                        .with(request -> {
                            SecurityContextHolder.getContext().setAuthentication(authenticationPrincipal());
                            return request;
                        }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-400"));
    }

    @DisplayName("내 게시완료 목록·인사이트 조회는 가짜 성공 대신 구현 중 응답을 반환한다")
    @Test
    void promptQueryEndpointsReturnNotImplemented() throws Exception {
        expectNotImplemented(get("/api/v1/prompts/me").param("status", "ACTIVE"));
        expectNotImplemented(get("/api/v1/prompts/me/insights"));
    }

    @DisplayName("생성과 임시저장은 500자 제목을 허용하고 공백이거나 500자를 초과하면 거절한다")
    @Test
    void promptTitleValidation() throws Exception {
        expectNotImplemented(put("/api/v1/prompts/draft")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"" + "가".repeat(Prompt.MAX_TITLE_LENGTH) + "\"}"));

        expectBadRequest(put("/api/v1/prompts/draft")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"   "}
                        """));

        expectBadRequest(put("/api/v1/prompts/draft")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"" + "가".repeat(Prompt.MAX_TITLE_LENGTH + 1) + "\"}"));

        expectBadRequest(post("/api/v1/prompts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validCreateRequest().replace(
                        "회의록 자동 정리",
                        "가".repeat(Prompt.MAX_TITLE_LENGTH + 1)
                )));
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
                          "description":"회의록을 정리합니다.",
                          "outputType":"IMAGE",
                          "jobTagIds":[1],
                          "taskTagIds":[2],
                          "aiModelTagIds":[3],
                          "contentType":"FREE",
                          "promptBody":"이미지를 생성해 주세요.",
                          "visibility":"PUBLIC",
                          "images":[
                            {"imageId":"123e4567-e89b-12d3-a456-426614174000","sortOrder":0,"thumbnail":true},
                            {"imageId":"123e4567-e89b-12d3-a456-426614174001","sortOrder":1,"thumbnail":true}
                          ]
                        }
                """));
    }

    @DisplayName("프롬프트 이미지 목록에 null 원소를 허용하지 않는다")
    @Test
    void nullImageIsRejected() throws Exception {
        expectBadRequest(post("/api/v1/prompts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title":"회의록 자동 정리",
                          "description":"회의록을 정리합니다.",
                          "outputType":"IMAGE",
                          "jobTagIds":[1],
                          "taskTagIds":[2],
                          "aiModelTagIds":[3],
                          "contentType":"FREE",
                          "promptBody":"이미지를 생성해 주세요.",
                          "visibility":"PUBLIC",
                          "images":[null]
                        }
                        """));
    }

    @DisplayName("설명, 직군·태스크·AI 모델, 공개 범위와 이미지는 생성 요청에 필수다")
    @Test
    void requiredPromptCreationFieldsAreRejectedWhenMissing() throws Exception {
        mockMvc.perform(post("/api/v1/prompts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"회의록 자동 정리",
                                  "outputType":"TEXT",
                                  "contentType":"FREE",
                                  "promptBody":"회의록을 정리해 주세요."
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-001"))
                .andExpect(jsonPath("$.result.description").exists())
                .andExpect(jsonPath("$.result.jobTagIds").exists())
                .andExpect(jsonPath("$.result.taskTagIds").exists())
                .andExpect(jsonPath("$.result.visibility").exists())
                .andExpect(jsonPath("$.result.images").exists());
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

    @DisplayName("WebP 업로드는 지원 범위에서 제외한다")
    @Test
    void webpUploadIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/prompt-images/upload-urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "images":[{
                                    "fileName":"result.webp",
                                    "contentType":"image/webp",
                                    "fileSize":1024,
                                    "width":1920,
                                    "height":1080
                                  }]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-001"))
                .andExpect(jsonPath("$.result['images[0].contentType']")
                        .value("이미지 형식은 JPEG 또는 PNG만 지원합니다."));
    }

    @DisplayName("게시완료 목록 조회는 status 값이 유효하지 않으면 400을 반환한다")
    @Test
    void getMyPublishedPromptsRejectsInvalidStatus() throws Exception {
        mockMvc.perform(get("/api/v1/prompts/me").param("status", "PUBLISHED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-001"));
    }

    @DisplayName("게시완료 목록 조회는 status 파라미터가 없으면 400을 반환한다")
    @Test
    void getMyPublishedPromptsRequiresStatus() throws Exception {
        mockMvc.perform(get("/api/v1/prompts/me"))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("게시완료 목록 조회는 size가 100을 초과하면 400을 반환한다")
    @Test
    void getMyPublishedPromptsRejectsOversizedPage() throws Exception {
        mockMvc.perform(get("/api/v1/prompts/me")
                        .param("status", "ACTIVE")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-400"));
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
                    "fileName":"prompt-result.jpg",
                    "contentType":"image/jpeg",
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
                  "visibility":"PUBLIC",
                  "images":[{
                    "imageId":"123e4567-e89b-12d3-a456-426614174000",
                    "sortOrder":0,
                    "thumbnail":true
                  }]
                }
                """;
    }

    private UsernamePasswordAuthenticationToken authenticationPrincipal() {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUserPrincipal(1L, "USER"),
                null,
                List.of()
        );
    }
}
