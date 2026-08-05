package com.promsearch.prompt.interfaces;

import com.promsearch.global.exception.NotImplementedException;
import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.response.PageResponse;
import com.promsearch.global.response.code.SuccessCode;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import com.promsearch.prompt.application.usecase.CompletePromptImageUploadUseCase;
import com.promsearch.prompt.application.usecase.CreatePromptUseCase;
import com.promsearch.prompt.application.usecase.DeletePromptDraftUseCase;
import com.promsearch.prompt.application.usecase.GetMyPromptInsightsUseCase;
import com.promsearch.prompt.application.usecase.GetPromptDraftUseCase;
import com.promsearch.prompt.application.usecase.GetPromptEditUseCase;
import com.promsearch.prompt.application.usecase.GetPromptDetailUseCase;
import com.promsearch.prompt.application.usecase.GetPromptImageStatusesUseCase;
import com.promsearch.prompt.application.usecase.IssuePromptImageUploadUrlsUseCase;
import com.promsearch.prompt.application.usecase.ListMyPromptsUseCase;
import com.promsearch.prompt.application.usecase.SavePromptDraftUseCase;
import com.promsearch.prompt.application.usecase.dto.CompletePromptImageUploadCommand;
import com.promsearch.prompt.application.usecase.dto.GetPromptImageStatusesQuery;
import com.promsearch.prompt.application.usecase.dto.ListMyPromptsQuery;
import com.promsearch.prompt.application.usecase.dto.MyPromptPageInfo;
import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.interfaces.docs.PromptControllerDocs;
import com.promsearch.prompt.interfaces.dto.request.CreatePromptRequest;
import com.promsearch.prompt.interfaces.dto.request.PromptImageUploadUrlRequest;
import com.promsearch.prompt.interfaces.dto.request.SavePromptDraftRequest;
import com.promsearch.prompt.interfaces.dto.response.MyPromptSummaryResponse;
import com.promsearch.prompt.interfaces.dto.response.PromptCommandResponse;
import com.promsearch.prompt.interfaces.dto.response.PromptDetailResponse;
import com.promsearch.prompt.interfaces.dto.response.PromptEditResponse;
import com.promsearch.prompt.interfaces.dto.response.PromptDraftResponse;
import com.promsearch.prompt.interfaces.dto.response.PromptImageStatusesResponse;
import com.promsearch.prompt.interfaces.dto.response.PromptImageUploadCompleteResponse;
import com.promsearch.prompt.interfaces.dto.response.PromptImageUploadUrlResponse;
import com.promsearch.prompt.interfaces.dto.response.PromptInsightResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class PromptController implements PromptControllerDocs {

    private final IssuePromptImageUploadUrlsUseCase issuePromptImageUploadUrlsUseCase;
    private final CompletePromptImageUploadUseCase completePromptImageUploadUseCase;
    private final CreatePromptUseCase createPromptUseCase;
    private final GetPromptDetailUseCase getPromptDetailUseCase;
    private final GetPromptImageStatusesUseCase getPromptImageStatusesUseCase;
    private final SavePromptDraftUseCase savePromptDraftUseCase;
    private final GetPromptDraftUseCase getPromptDraftUseCase;
    private final GetPromptEditUseCase getPromptEditUseCase;
    private final DeletePromptDraftUseCase deletePromptDraftUseCase;
    private final ListMyPromptsUseCase listMyPromptsUseCase;
    private final GetMyPromptInsightsUseCase getMyPromptInsightsUseCase;

    @GetMapping("/prompts/{promptId}")
    @Override
    public ApiResponse<PromptDetailResponse> getPromptDetail(
            @PathVariable Long promptId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal user
    ) {
        return ApiResponse.onSuccess(PromptDetailResponse.from(
                getPromptDetailUseCase.get(promptId, user == null ? null : user.userId())
        ));
    }

    @GetMapping("/prompts/{promptId}/edit")
    @Override
    public ApiResponse<PromptEditResponse> getPromptEdit(
            @PathVariable Long promptId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal user
    ) {
        return ApiResponse.onSuccess(PromptEditResponse.from(
                getPromptEditUseCase.get(promptId, user.userId())
        ));
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

    /** 인증 사용자 이미지 처리 상태를 요청 순서대로 일괄 조회 */
    @GetMapping("/prompt-images/statuses")
    @Override
    public ApiResponse<PromptImageStatusesResponse> getImageStatuses(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @RequestParam("imageIds") @Size(min = 1, max = 10, message = "imageIds size must be between 1 and 10")
            List<UUID> imageIds
    ) {
        return ApiResponse.onSuccess(PromptImageStatusesResponse.from(
                getPromptImageStatusesUseCase.getStatuses(
                        new GetPromptImageStatusesQuery(user.userId(), imageIds)
                )
        ));
    }

    @PutMapping("/prompts/draft")
    @Override
    public ApiResponse<PromptCommandResponse> saveDraft(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @Valid @RequestBody SavePromptDraftRequest request
    ) {
        return ApiResponse.onSuccess(PromptCommandResponse.from(
                savePromptDraftUseCase.save(request.toCommand(user.userId()))
        ));
    }

    @GetMapping("/prompts/draft")
    @Override
    public ApiResponse<PromptDraftResponse> getDraft(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user
    ) {
        return ApiResponse.onSuccess(PromptDraftResponse.from(
                getPromptDraftUseCase.get(user.userId())
        ));
    }

    @DeleteMapping("/prompts/draft")
    @Override
    public ApiResponse<Void> deleteDraft(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user
    ) {
        deletePromptDraftUseCase.delete(user.userId());
        return ApiResponse.onSuccess(null);
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

    @GetMapping("/prompts/me")
    @Override
    public ApiResponse<PageResponse<MyPromptSummaryResponse>> getMyPrompts(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @RequestParam PromptStatus status,
            @Min(value = 0, message = "page must be 0 or greater")
            @RequestParam(defaultValue = "0") int page,
            @Min(value = 1, message = "size must be 1 or greater")
            @Max(value = 100, message = "size must be 100 or less")
            @RequestParam(defaultValue = "20") int size
    ) {
        MyPromptPageInfo pageInfo = listMyPromptsUseCase.listMyPrompts(
                ListMyPromptsQuery.of(user.userId(), status, page, size)
        );
        List<MyPromptSummaryResponse> content = pageInfo.content().stream()
                .map(MyPromptSummaryResponse::from)
                .toList();
        return ApiResponse.onSuccess(PageResponse.of(content, page, size, pageInfo.totalElements()));
    }

    @GetMapping("/prompts/me/insights")
    @Override
    public ApiResponse<PromptInsightResponse> getMyPromptInsights(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user
    ) {
        return ApiResponse.onSuccess(
                PromptInsightResponse.from(getMyPromptInsightsUseCase.getMyPromptInsights(user.userId()))
        );
    }
}
