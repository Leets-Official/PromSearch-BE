package com.promsearch.prompt.application.usecase.dto;

import com.promsearch.prompt.domain.enums.TagType;

public record HomePromptTagInfo(
        Long tagId,
        TagType tagType,
        String name
) {
}
