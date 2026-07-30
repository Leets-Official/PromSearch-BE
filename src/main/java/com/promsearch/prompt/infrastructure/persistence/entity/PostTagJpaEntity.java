package com.promsearch.prompt.infrastructure.persistence.entity;

import com.promsearch.common.BaseEntity;
import com.promsearch.prompt.domain.PostTag;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "post_tags")
public class PostTagJpaEntity extends BaseEntity {

    @EmbeddedId
    private PostTagId id;

    @MapsId("postId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private PostJpaEntity post;

    @MapsId("tagId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private TagJpaEntity tag;

    @Builder(access = AccessLevel.PRIVATE)
    private PostTagJpaEntity(PostJpaEntity post, TagJpaEntity tag) {
        this.post = post;
        this.tag = tag;
        this.id = PostTagId.create(post.getId(), tag.getId());
    }

    public static PostTagJpaEntity create(PostJpaEntity post, TagJpaEntity tag) {
        return PostTagJpaEntity.builder()
                .post(post)
                .tag(tag)
                .build();
    }

    public PostTag toDomain() {
        return PostTag.reconstruct(post.getId(), tag.getId());
    }
}
