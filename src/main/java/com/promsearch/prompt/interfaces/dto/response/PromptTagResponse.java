package com.promsearch.prompt.interfaces.dto.response;

import com.promsearch.prompt.application.usecase.dto.PromptTagInfo;
import com.promsearch.prompt.domain.enums.TagType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프롬프트 태그")
public record PromptTagResponse(
        @Schema(description = "태그 ID", example = "4")
        Long tagId,

        @Schema(description = "태그 종류", example = "AI_MODEL")
        TagType tagType,

        @Schema(description = "태그 이름", example = "ChatGPT")
        String name
) {

    public static PromptTagResponse from(PromptTagInfo info) {
        return new PromptTagResponse(info.tagId(), info.tagType(), info.name());
    }
}
