package com.promsearch.community.interfaces;

import com.promsearch.community.application.usecase.LikePromptUseCase;
import com.promsearch.community.application.usecase.UnlikePromptUseCase;
import com.promsearch.community.application.usecase.dto.LikePromptCommand;
import com.promsearch.community.interfaces.docs.LikeControllerDocs;
import com.promsearch.community.interfaces.dto.response.LikeResponse;
import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.response.code.SuccessCode;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/prompts/{promptId}/likes")
public class LikeController implements LikeControllerDocs {

    private final LikePromptUseCase likePromptUseCase;
    private final UnlikePromptUseCase unlikePromptUseCase;

    @PostMapping
    @Override
    public ResponseEntity<ApiResponse<LikeResponse>> like(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @PathVariable Long promptId
    ) {
        LikeResponse response = LikeResponse.from(likePromptUseCase.like(
                new LikePromptCommand(user.userId(), promptId)
        ));
        return ResponseEntity
                .status(SuccessCode.CREATED.getHttpStatus())
                .body(ApiResponse.onSuccess(SuccessCode.CREATED, response));
    }

    @DeleteMapping
    @Override
    public ApiResponse<LikeResponse> unlike(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @PathVariable Long promptId
    ) {
        return ApiResponse.onSuccess(LikeResponse.from(unlikePromptUseCase.unlike(
                new LikePromptCommand(user.userId(), promptId)
        )));
    }
}
