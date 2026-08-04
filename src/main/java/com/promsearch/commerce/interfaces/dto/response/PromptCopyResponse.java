package com.promsearch.commerce.interfaces.dto.response;

import com.promsearch.commerce.application.usecase.dto.CopyPromptInfo;
import io.swagger.v3.oas.annotations.media.Schema;

public record PromptCopyResponse(
        @Schema(description = "복사한 프롬프트 ID", example = "10")
        Long promptId,

        @Schema(description = "복사 권한이 확인된 프롬프트 전문", example = "회의록을 요약해 주세요.")
        String promptBody
) {

    public static PromptCopyResponse from(CopyPromptInfo info) {
        return new PromptCopyResponse(info.promptId(), info.promptBody());
    }
}
