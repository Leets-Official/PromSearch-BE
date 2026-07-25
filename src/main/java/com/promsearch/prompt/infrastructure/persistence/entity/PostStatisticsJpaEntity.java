package com.promsearch.prompt.infrastructure.persistence.entity;

import com.promsearch.common.BaseEntity;
import com.promsearch.prompt.domain.PostStatistics;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "post_statistics")
public class PostStatisticsJpaEntity extends BaseEntity {

    @Id
    @Column(name = "post_id")
    private Long postId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private PostJpaEntity post;

    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    @Column(name = "copy_count", nullable = false)
    private Long copyCount;

    @Column(name = "like_count", nullable = false)
    private Long likeCount;

    @Column(name = "report_count", nullable = false)
    private Long reportCount;

    @Column(name = "comment_count", nullable = false)
    private Long commentCount;

    @Builder(access = AccessLevel.PRIVATE)
    private PostStatisticsJpaEntity(PostJpaEntity post) {
        this.post = post;
        this.viewCount = 0L;
        this.copyCount = 0L;
        this.likeCount = 0L;
        this.reportCount = 0L;
        this.commentCount = 0L;
    }

    public static PostStatisticsJpaEntity create(PostJpaEntity post) {
        return PostStatisticsJpaEntity.builder()
                .post(post)
                .build();
    }

    public PostStatistics toDomain() {
        return PostStatistics.reconstruct(
                postId,
                viewCount,
                copyCount,
                likeCount,
                reportCount,
                commentCount
        );
    }
}
