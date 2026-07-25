package com.promsearch.prompt.interfaces.dto;

import com.promsearch.prompt.application.HomePromptTagInfo;
import com.promsearch.prompt.domain.enums.TagType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "홈 프롬프트 카드에 표시되는 태그")
public record HomePromptTagResponse(
        @Schema(description = "태그 ID", example = "4")
        Long tagId,

        @Schema(description = "태그 타입", example = "JOB")
        TagType tagType,

        @Schema(description = "표시 이름", example = "디자이너")
        String name
) {

    public static HomePromptTagResponse from(HomePromptTagInfo info) {
        return new HomePromptTagResponse(info.tagId(), info.tagType(), info.name());
    }
}
