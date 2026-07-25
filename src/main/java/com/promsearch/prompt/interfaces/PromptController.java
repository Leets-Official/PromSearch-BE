package com.promsearch.prompt.interfaces;

import com.promsearch.global.exception.NotImplementedException;
import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import com.promsearch.prompt.interfaces.docs.PromptControllerDocs;
import com.promsearch.prompt.interfaces.dto.request.CreatePromptRequest;
import com.promsearch.prompt.interfaces.dto.response.PromptCommandResponse;
import com.promsearch.prompt.interfaces.dto.response.PromptDraftResponse;
import com.promsearch.prompt.interfaces.dto.request.PromptImageUploadUrlRequest;
import com.promsearch.prompt.interfaces.dto.response.PromptImageUploadUrlResponse;
import com.promsearch.prompt.interfaces.dto.request.SavePromptDraftRequest;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1")
public class PromptController implements PromptControllerDocs {

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
}
