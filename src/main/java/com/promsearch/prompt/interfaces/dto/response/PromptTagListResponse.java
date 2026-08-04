package com.promsearch.prompt.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "프롬프트 태그 목록")
public record PromptTagListResponse(
        @Schema(description = "태그 목록")
        List<PromptTagResponse> tags
) {
}
