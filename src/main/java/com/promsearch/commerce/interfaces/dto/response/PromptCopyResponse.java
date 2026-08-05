package com.promsearch.commerce.interfaces.dto.response;

import com.promsearch.commerce.application.usecase.dto.CopyPromptInfo;
import io.swagger.v3.oas.annotations.media.Schema;

public record PromptCopyResponse(
        @Schema(description = "복사한 프롬프트 ID", example = "10")
        Long promptId,

        @Schema(description = "복사 처리 후 프롬프트의 누적 복사 수", example = "16")
        long copyCount
) {

    public static PromptCopyResponse from(CopyPromptInfo info) {
        return new PromptCopyResponse(info.promptId(), info.copyCount());
    }
}
