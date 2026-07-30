package com.promsearch.prompt.infrastructure.persistence.entity;

import com.promsearch.common.BaseEntity;
import com.promsearch.prompt.domain.Prompt;
import com.promsearch.prompt.domain.Prompt.PromptId;
import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.domain.enums.PromptVisibility;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "posts")
public class PostJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "prompt_body", columnDefinition = "TEXT")
    private String promptBody;

    /*
     * TODO(#33): 프롬프트 생성·조회 전환이 끝나면 prompt_images.is_thumbnail에서 대표 이미지를 찾고
     * 이 중복 URL 컬럼은 제거한다. URL 대신 Object Key를 기준으로 조회해야 한다.
     */
    @Column(name = "thumbnail_image_url", length = 500)
    private String thumbnailImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "output_type", length = 20)
    private PromptOutputType outputType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", length = 20)
    private PromptContentType contentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PromptStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private PromptVisibility visibility;

    @Column(name = "price_point")
    private Long pricePoint;

    @Column(name = "hidden_reason", length = 255)
    private String hiddenReason;

    @Column(name = "hidden_at")
    private Instant hiddenAt;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostTagJpaEntity> postTags = new ArrayList<>();

    @OneToOne(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private PostStatisticsJpaEntity statistics;

    @Builder(access = AccessLevel.PRIVATE)
    private PostJpaEntity(Long userId, String title, String promptBody, String thumbnailImageUrl,
                          PromptOutputType outputType, String description, PromptContentType contentType,
                          Long pricePoint) {
        this.userId = userId;
        this.title = title;
        this.promptBody = promptBody;
        this.thumbnailImageUrl = thumbnailImageUrl;
        this.outputType = outputType;
        this.description = description;
        this.contentType = contentType;
        this.status = PromptStatus.DRAFT;
        this.visibility = PromptVisibility.PUBLIC;
        this.pricePoint = pricePoint != null ? pricePoint : 0L;
    }

    public static PostJpaEntity create(Long userId, String title, String promptBody, String thumbnailImageUrl,
                                       PromptOutputType outputType, String description,
                                       PromptContentType contentType, Long pricePoint) {
        return PostJpaEntity.builder()
                .userId(userId)
                .title(title)
                .promptBody(promptBody)
                .thumbnailImageUrl(thumbnailImageUrl)
                .outputType(outputType)
                .description(description)
                .contentType(contentType)
                .pricePoint(pricePoint)
                .build();
    }

    public Prompt toDomain() {
        return Prompt.reconstruct(
                new PromptId(id),
                userId,
                title,
                promptBody,
                thumbnailImageUrl,
                outputType,
                description,
                contentType,
                status,
                pricePoint,
                hiddenReason,
                hiddenAt,
                getCreatedAt(),
                getUpdatedAt(),
                getDeletedAt(),
                // PromptImage는 독립 Aggregate이므로 조회 어댑터에서 필요한 경우 별도로 일괄 조회한다.
                List.of(),
                statistics != null ? statistics.toDomain() : null,
                postTags.stream()
                        .map(PostTagJpaEntity::toDomain)
                        .toList()
        );
    }

    public void addPostTag(PostTagJpaEntity postTag) {
        this.postTags.add(postTag);
    }

    public void initializeStatistics(PostStatisticsJpaEntity statistics) {
        this.statistics = statistics;
    }
}
