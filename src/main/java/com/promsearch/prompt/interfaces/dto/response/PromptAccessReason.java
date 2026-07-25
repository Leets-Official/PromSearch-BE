package com.promsearch.prompt.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프롬프트 본문 잠금 사유")
public enum PromptAccessReason {
    ANONYMOUS,
    PREMIUM
}
