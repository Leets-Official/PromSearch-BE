package com.promsearch.user.application.port.out.tag;

import com.promsearch.user.domain.enums.InterestTagType;

public record InterestTagRow(Long tagId, InterestTagType type, String tagName) {
}
