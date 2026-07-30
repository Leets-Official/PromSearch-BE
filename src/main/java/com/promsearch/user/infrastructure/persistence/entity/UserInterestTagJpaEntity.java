package com.promsearch.user.infrastructure.persistence.entity;

import com.promsearch.prompt.infrastructure.persistence.entity.TagJpaEntity;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
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

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity user;

    @MapsId("tagId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private TagJpaEntity tag;

    private UserInterestTagJpaEntity(UserJpaEntity user, TagJpaEntity tag) {
        this.id = new UserInterestTagId(user.getId(), tag.getId());
        this.user = user;
        this.tag = tag;
    }

    public static UserInterestTagJpaEntity create(UserJpaEntity user, TagJpaEntity tag) {
        return new UserInterestTagJpaEntity(user, tag);
    }
}
