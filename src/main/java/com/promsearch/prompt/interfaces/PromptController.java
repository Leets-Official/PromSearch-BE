package com.promsearch.prompt.interfaces;

import com.promsearch.global.exception.NotImplementedException;
import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.response.code.SuccessCode;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import com.promsearch.prompt.application.usecase.CompletePromptImageUploadUseCase;
import com.promsearch.prompt.application.usecase.CreatePromptUseCase;
import com.promsearch.prompt.application.usecase.IssuePromptImageUploadUrlsUseCase;
import com.promsearch.prompt.application.usecase.dto.CompletePromptImageUploadCommand;
import com.promsearch.prompt.interfaces.docs.PromptControllerDocs;
import com.promsearch.prompt.interfaces.dto.request.CreatePromptRequest;
import com.promsearch.prompt.interfaces.dto.request.PromptImageUploadUrlRequest;
import com.promsearch.prompt.interfaces.dto.request.SavePromptDraftRequest;
import com.promsearch.prompt.interfaces.dto.response.PromptCommandResponse;
import com.promsearch.prompt.interfaces.dto.response.PromptDetailResponse;
import com.promsearch.prompt.interfaces.dto.response.PromptDraftResponse;
import com.promsearch.prompt.interfaces.dto.response.PromptImageUploadCompleteResponse;
import com.promsearch.prompt.interfaces.dto.response.PromptImageUploadUrlResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class PromptController implements PromptControllerDocs {

    private final IssuePromptImageUploadUrlsUseCase issuePromptImageUploadUrlsUseCase;
    private final CompletePromptImageUploadUseCase completePromptImageUploadUseCase;
    private final CreatePromptUseCase createPromptUseCase;

    @GetMapping("/prompts/{promptId}")
    @Override
    public ApiResponse<PromptDetailResponse> getPromptDetail(
            @PathVariable Long promptId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal user
    ) {
        throw new NotImplementedException("프롬프트 상세 조회 기능은 아직 구현되지 않았습니다.");
    }

    /** 업로드 요청 DTO 변환 및 Presigned URL 발급 응답 반환 */
    @PostMapping("/prompt-images/upload-urls")
    @Override
    public ApiResponse<PromptImageUploadUrlResponse> issueImageUploadUrls(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @Valid @RequestBody PromptImageUploadUrlRequest request
    ) {
        return ApiResponse.onSuccess(PromptImageUploadUrlResponse.from(
                issuePromptImageUploadUrlsUseCase.issue(request.toCommand(user.userId()))
        ));
    }

    /** 인증 사용자·이미지 식별자 기반 S3 업로드 완료 검증 */
    @PostMapping("/prompt-images/{imageId}/complete")
    @Override
    public ApiResponse<PromptImageUploadCompleteResponse> completeImageUpload(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @PathVariable UUID imageId
    ) {
        return ApiResponse.onSuccess(PromptImageUploadCompleteResponse.from(
                completePromptImageUploadUseCase.complete(
                        new CompletePromptImageUploadCommand(user.userId(), imageId)
                )
        ));
    }

    @PutMapping("/prompts/draft")
    @Override
    public ApiResponse<PromptCommandResponse> saveDraft(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @Valid @RequestBody SavePromptDraftRequest request
    ) {
        throw new NotImplementedException();
    }

    @GetMapping("/prompts/draft")
    @Override
    public ApiResponse<PromptDraftResponse> getDraft(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user
    ) {
        throw new NotImplementedException();
    }

    @DeleteMapping("/prompts/draft")
    @Override
    public ApiResponse<Void> deleteDraft(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user
    ) {
        throw new NotImplementedException();
    }

    @PostMapping("/prompts")
    @Override
    public ResponseEntity<ApiResponse<PromptCommandResponse>> createPrompt(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @Valid @RequestBody CreatePromptRequest request
    ) {
        PromptCommandResponse response = PromptCommandResponse.from(
                createPromptUseCase.create(request.toCommand(user.userId()))
        );
        return ResponseEntity
                .status(SuccessCode.CREATED.getHttpStatus())
                .body(ApiResponse.onSuccess(SuccessCode.CREATED, response));
    }

    @DeleteMapping("/prompts/{promptId}")
    @Override
    public ApiResponse<Void> deletePrompt(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @PathVariable Long promptId
    ) {
        throw new NotImplementedException();
    }
}
