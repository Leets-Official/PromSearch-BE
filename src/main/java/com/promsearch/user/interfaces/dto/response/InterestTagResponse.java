package com.promsearch.user.interfaces.dto.response;

import com.promsearch.user.application.usecase.dto.InterestTagInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관심 태그")
public record InterestTagResponse(
        @Schema(description = "태그 식별자", example = "1")
        Long tagId,
        @Schema(description = "태그명", example = "직장인")
        String tagName
) {

    public static InterestTagResponse from(InterestTagInfo info) {
        return new InterestTagResponse(info.tagId(), info.tagName());
    }
}
