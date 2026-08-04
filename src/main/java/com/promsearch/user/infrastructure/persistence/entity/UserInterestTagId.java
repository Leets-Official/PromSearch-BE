package com.promsearch.user.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public record UserInterestTagId(
        @Column(name = "user_id", nullable = false)
        Long userId,

        @Column(name = "tag_id", nullable = false)
        Long tagId
) implements Serializable {
}
