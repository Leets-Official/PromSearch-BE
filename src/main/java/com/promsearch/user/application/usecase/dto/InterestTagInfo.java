package com.promsearch.user.application.usecase.dto;

import com.promsearch.user.domain.enums.InterestTagType;

public record InterestTagInfo(Long tagId, String name, InterestTagType type) {
}
