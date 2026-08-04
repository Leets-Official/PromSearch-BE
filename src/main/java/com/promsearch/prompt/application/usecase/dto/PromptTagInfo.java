package com.promsearch.prompt.application.usecase.dto;

import com.promsearch.prompt.domain.Tag;
import com.promsearch.prompt.domain.enums.TagType;

/*
 * interfaces 계층으로 내려보낼 태그 조회 결과입니다.
 * 화면은 tagId를 다시 필터 요청 값으로 보내므로 ID, 타입, 표시 이름을 함께 제공합니다.
 */
public record PromptTagInfo(
        Long tagId,
        TagType tagType,
        String name
) {

    public static PromptTagInfo from(Tag tag) {
        return new PromptTagInfo(
                tag.getTagId().id(),
                tag.getTagType(),
                tag.getTagName()
        );
    }
}
