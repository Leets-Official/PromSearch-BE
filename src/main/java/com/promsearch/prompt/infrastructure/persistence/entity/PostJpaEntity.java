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
import java.util.Objects;
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

    @Column(name = "title", nullable = false, length = Prompt.MAX_TITLE_LENGTH)
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

    @Column(name = "published_at")
    private Instant publishedAt;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostTagJpaEntity> postTags = new ArrayList<>();

    @OneToOne(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private PostStatisticsJpaEntity statistics;

    @Builder(access = AccessLevel.PRIVATE)
    private PostJpaEntity(Long userId, String title, String promptBody, String thumbnailImageUrl,
                          PromptOutputType outputType, String description, PromptContentType contentType,
                          PromptStatus status, PromptVisibility visibility, Long pricePoint) {
        this.userId = userId;
        this.title = title;
        this.promptBody = promptBody;
        this.thumbnailImageUrl = thumbnailImageUrl;
        this.outputType = outputType;
        this.description = description;
        this.contentType = contentType;
        this.status = status;
        this.visibility = visibility;
        this.pricePoint = pricePoint != null ? pricePoint : 0L;
    }

    public static PostJpaEntity from(Prompt prompt) {
        return PostJpaEntity.builder()
                .userId(prompt.getUserId())
                .title(prompt.getTitle())
                .promptBody(prompt.getPromptBody())
                .thumbnailImageUrl(null)
                .outputType(prompt.getOutputType())
                .description(prompt.getDescription())
                .contentType(prompt.getContentType())
                .status(prompt.getStatus())
                .visibility(prompt.getVisibility())
                .pricePoint(prompt.getPricePoint())
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
                visibility,
                pricePoint,
                hiddenReason,
                hiddenAt,
                getCreatedAt(),
                getUpdatedAt(),
                getDeletedAt(),
                publishedAt,
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

    public void replaceDraft(Prompt draft, List<TagJpaEntity> tags) {
        if (draft.getStatus() != PromptStatus.DRAFT) {
            throw new IllegalArgumentException("DRAFT 상태만 임시저장으로 교체할 수 있습니다.");
        }

        title = draft.getTitle();
        promptBody = draft.getPromptBody();
        thumbnailImageUrl = null;
        outputType = draft.getOutputType();
        description = draft.getDescription();
        contentType = draft.getContentType();
        status = PromptStatus.DRAFT;
        visibility = draft.getVisibility();
        pricePoint = draft.getPricePoint() != null ? draft.getPricePoint() : 0L;
        hiddenReason = null;
        hiddenAt = null;
        postTags.removeIf(postTag -> tags.stream()
                .noneMatch(tag -> Objects.equals(tag.getId(), postTag.getTag().getId())));
        for (TagJpaEntity tag : tags) {
            boolean alreadyAttached = postTags.stream()
                    .anyMatch(postTag -> Objects.equals(postTag.getTag().getId(), tag.getId()));
            if (!alreadyAttached) {
                addPostTag(PostTagJpaEntity.create(this, tag));
            }
        }
        statistics = null;
    }

    public void initializeStatistics(PostStatisticsJpaEntity statistics) {
        this.statistics = statistics;
    }

    public void deleteDraft() {
        if (status != PromptStatus.DRAFT) {
            throw new IllegalArgumentException("DRAFT 상태만 임시저장에서 삭제할 수 있습니다.");
        }
        markDeleted();
    }

    public void delete() {
        markDeleted();
    }

    public void hide() {
        if (status == PromptStatus.DELETED) {
            return;
        }
        status = PromptStatus.HIDDEN;
        hiddenAt = Instant.now();
    }
}
