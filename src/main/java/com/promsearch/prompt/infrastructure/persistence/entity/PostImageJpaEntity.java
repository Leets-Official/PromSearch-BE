package com.promsearch.prompt.infrastructure.persistence.entity;

import com.promsearch.common.BaseEntity;
import com.promsearch.prompt.domain.PostImage;
import com.promsearch.prompt.domain.PostImage.PostImageId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "post_image")
public class PostImageJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_image_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private PostJpaEntity post;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "is_thumbnail", nullable = false)
    private Boolean thumbnail;

    @Builder(access = AccessLevel.PRIVATE)
    private PostImageJpaEntity(PostJpaEntity post, String imageUrl, Integer sortOrder, Boolean thumbnail) {
        this.post = post;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder != null ? sortOrder : 0;
        this.thumbnail = thumbnail != null ? thumbnail : false;
    }

    public static PostImageJpaEntity create(PostJpaEntity post, String imageUrl, Integer sortOrder, Boolean thumbnail) {
        return PostImageJpaEntity.builder()
                .post(post)
                .imageUrl(imageUrl)
                .sortOrder(sortOrder)
                .thumbnail(thumbnail)
                .build();
    }

    public PostImage toDomain() {
        return PostImage.reconstruct(
                new PostImageId(id),
                post.getId(),
                imageUrl,
                sortOrder,
                thumbnail,
                getCreatedAt()
        );
    }
}
