package com.promsearch.user.application.usecase.dto;

import com.promsearch.user.application.port.out.tag.InterestTagRow;

public record InterestTagInfo(Long tagId, String tagName) {

    public static InterestTagInfo from(InterestTagRow row) {
        return new InterestTagInfo(row.tagId(), row.tagName());
    }
}
