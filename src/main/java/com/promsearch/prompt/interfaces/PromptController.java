package com.promsearch.prompt.interfaces;

import com.promsearch.global.exception.NotImplementedException;
import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import com.promsearch.prompt.interfaces.docs.PromptControllerDocs;
import com.promsearch.prompt.interfaces.dto.PromptDetailResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/prompts")
public class PromptController implements PromptControllerDocs {

    @GetMapping("/{promptId}")
    @Override
    public ApiResponse<PromptDetailResponse> getPromptDetail(
            @PathVariable Long promptId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal user
    ) {
        throw new NotImplementedException("프롬프트 상세 조회 기능은 아직 구현되지 않았습니다.");
    }
}
