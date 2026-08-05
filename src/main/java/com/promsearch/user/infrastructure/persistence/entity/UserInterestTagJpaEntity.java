package com.promsearch.user.infrastructure.persistence.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_interest_tags",
        indexes = @Index(name = "idx_user_interest_tags_tag_id", columnList = "tag_id")
)
public class UserInterestTagJpaEntity {

    @EmbeddedId
    private UserInterestTagId id;

    private UserInterestTagJpaEntity(Long userId, Long tagId) {
        this.id = new UserInterestTagId(userId, tagId);
    }

    public static UserInterestTagJpaEntity create(Long userId, Long tagId) {
        return new UserInterestTagJpaEntity(userId, tagId);
    }
}
