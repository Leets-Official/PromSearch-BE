package com.promsearch.prompt.interfaces;

import com.promsearch.global.exception.NotImplementedException;
import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.response.PageResponse;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.interfaces.docs.PromptControllerDocs;
import com.promsearch.prompt.interfaces.dto.request.CreatePromptRequest;
import com.promsearch.prompt.interfaces.dto.request.PromptImageUploadUrlRequest;
import com.promsearch.prompt.interfaces.dto.request.SavePromptDraftRequest;
import com.promsearch.prompt.interfaces.dto.response.MyPromptSummaryResponse;
import com.promsearch.prompt.interfaces.dto.response.PromptCommandResponse;
import com.promsearch.prompt.interfaces.dto.response.PromptDetailResponse;
import com.promsearch.prompt.interfaces.dto.response.PromptDraftResponse;
import com.promsearch.prompt.interfaces.dto.response.PromptImageUploadUrlResponse;
import com.promsearch.prompt.interfaces.dto.response.PromptInsightResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
@RequestMapping("/api/v1")
public class PromptController implements PromptControllerDocs {

    @GetMapping("/prompts/{promptId}")
    @Override
    public ApiResponse<PromptDetailResponse> getPromptDetail(
            @PathVariable Long promptId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal user
    ) {
        throw new NotImplementedException("프롬프트 상세 조회 기능은 아직 구현되지 않았습니다.");
    }

    @PostMapping("/prompt-images/upload-urls")
    @Override
    public ApiResponse<PromptImageUploadUrlResponse> issueImageUploadUrls(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @Valid @RequestBody PromptImageUploadUrlRequest request
    ) {
        throw new NotImplementedException();
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
        throw new NotImplementedException();
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
    public ApiResponse<PageResponse<MyPromptSummaryResponse>> getMyPublishedPrompts(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @RequestParam PromptStatus status,
            @Min(value = 0, message = "page must be 0 or greater")
            @RequestParam(defaultValue = "0") int page,
            @Min(value = 1, message = "size must be 1 or greater")
            @Max(value = 100, message = "size must be 100 or less")
            @RequestParam(defaultValue = "20") int size
    ) {
        throw new NotImplementedException();
    }

    @GetMapping("/prompts/me/insights")
    @Override
    public ApiResponse<PromptInsightResponse> getMyPromptInsights(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user
    ) {
        throw new NotImplementedException();
    }
}
