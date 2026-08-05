package com.promsearch.user.application.usecase.dto;

import com.promsearch.user.application.port.out.tag.InterestTagRow;
import com.promsearch.user.domain.enums.InterestTagType;

public record InterestTagInfo(Long tagId, String tagName, InterestTagType type) {

    public InterestTagInfo(Long tagId, String tagName) {
        this(tagId, tagName, null);
    }

    public static InterestTagInfo from(InterestTagRow row) {
        return new InterestTagInfo(row.tagId(), row.tagName(), row.type());
    }
}
