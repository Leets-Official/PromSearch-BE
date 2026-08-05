package com.promsearch.prompt.infrastructure.persistence;

import com.promsearch.prompt.domain.enums.TagType;

public interface UserInterestTagProjection {

    Long getTagId();

    TagType getType();

    String getTagName();
}
