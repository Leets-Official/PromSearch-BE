package com.promsearch.commerce.interfaces;

import com.promsearch.commerce.application.usecase.CopyPromptUseCase;
import com.promsearch.commerce.application.usecase.UnlockPromptUseCase;
import com.promsearch.commerce.application.usecase.dto.CopyPromptCommand;
import com.promsearch.commerce.application.usecase.dto.UnlockPromptCommand;
import com.promsearch.commerce.interfaces.docs.PromptAccessControllerDocs;
import com.promsearch.commerce.interfaces.dto.response.PromptCopyResponse;
import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/prompts/{promptId}")
public class PromptAccessController implements PromptAccessControllerDocs {

    private final UnlockPromptUseCase unlockPromptUseCase;
    private final CopyPromptUseCase copyPromptUseCase;

    @PostMapping("/unlock")
    @Override
    public ApiResponse<Void> unlock(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @PathVariable Long promptId
    ) {
        unlockPromptUseCase.unlock(new UnlockPromptCommand(user.userId(), promptId));
        return ApiResponse.onSuccess(null);
    }

    @PostMapping("/copy")
    @Override
    public ApiResponse<PromptCopyResponse> copy(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @PathVariable Long promptId
    ) {
        return ApiResponse.onSuccess(PromptCopyResponse.from(copyPromptUseCase.copy(
                new CopyPromptCommand(user.userId(), promptId)
        )));
    }
}
