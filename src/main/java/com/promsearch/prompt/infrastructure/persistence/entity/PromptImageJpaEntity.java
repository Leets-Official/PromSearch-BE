package com.promsearch.prompt.infrastructure.persistence.entity;

import com.promsearch.common.BaseEntity;
import com.promsearch.prompt.domain.PromptImage;
import com.promsearch.prompt.domain.PromptImage.PromptImageId;
import com.promsearch.prompt.domain.enums.PromptImageContentType;
import com.promsearch.prompt.domain.enums.PromptImageStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "prompt_images",
        indexes = {
                @Index(name = "idx_prompt_images_uploader_status", columnList = "uploader_id,status"),
                @Index(name = "idx_prompt_images_status_created_at", columnList = "status,created_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_prompt_images_original_object_key",
                        columnNames = "original_object_key"
                ),
                @UniqueConstraint(
                        name = "uk_prompt_images_watermarked_object_key",
                        columnNames = "watermarked_object_key"
                ),
                @UniqueConstraint(
                        name = "uk_prompt_images_prompt_sort_order",
                        columnNames = {"prompt_id", "sort_order"}
                )
        }
)
public class PromptImageJpaEntity extends BaseEntity {

    @Id
    @Column(name = "prompt_image_id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "uploader_id", nullable = false, updatable = false)
    private Long uploaderId;

    /*
     * 업로드 시점에는 프롬프트가 아직 없으므로 nullable이다.
     * PromptImage를 독립 Aggregate로 유지하기 위해 PostJpaEntity 연관관계 대신 ID만 참조한다.
     */
    @Column(name = "prompt_id")
    private Long promptId;

    @Column(name = "original_object_key", nullable = false, updatable = false, length = 1_024)
    private String originalObjectKey;

    @Column(name = "watermarked_object_key", length = 1_024)
    private String watermarkedObjectKey;

    @Column(name = "original_file_name", nullable = false, updatable = false, length = 255)
    private String originalFileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, updatable = false, length = 10)
    private PromptImageContentType contentType;

    @Column(name = "file_size", nullable = false, updatable = false)
    private Long fileSize;

    @Column(name = "width", nullable = false, updatable = false)
    private Integer width;

    @Column(name = "height", nullable = false, updatable = false)
    private Integer height;

    @Column(name = "etag", length = 255)
    private String etag;

    @Column(name = "uploaded_at")
    private Instant uploadedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PromptImageStatus status;

    @Column(name = "processing_version", nullable = false)
    private Integer processingVersion;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "is_thumbnail", nullable = false)
    private Boolean thumbnail;

    /*
     * Worker 상태 갱신과 프롬프트 생성의 동시 실행을 감지한다.
     * processing_version은 워터마크 알고리즘 버전이고, lock_version은 DB 동시성 제어용이다.
     */
    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion;

    @Builder(access = AccessLevel.PRIVATE)
    private PromptImageJpaEntity(
            UUID id,
            Long uploaderId,
            Long promptId,
            String originalObjectKey,
            String watermarkedObjectKey,
            String originalFileName,
            PromptImageContentType contentType,
            Long fileSize,
            Integer width,
            Integer height,
            String etag,
            Instant uploadedAt,
            PromptImageStatus status,
            Integer processingVersion,
            String failureCode,
            Integer sortOrder,
            Boolean thumbnail
    ) {
        this.id = id;
        this.uploaderId = uploaderId;
        this.promptId = promptId;
        this.originalObjectKey = originalObjectKey;
        this.watermarkedObjectKey = watermarkedObjectKey;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.width = width;
        this.height = height;
        this.etag = etag;
        this.uploadedAt = uploadedAt;
        this.status = status;
        this.processingVersion = processingVersion;
        this.failureCode = failureCode;
        this.sortOrder = sortOrder;
        this.thumbnail = thumbnail;
    }

    public static PromptImageJpaEntity from(PromptImage image) {
        return PromptImageJpaEntity.builder()
                .id(image.getPromptImageId().id())
                .uploaderId(image.getUploaderId())
                .promptId(image.getPromptId())
                .originalObjectKey(image.getOriginalObjectKey())
                .watermarkedObjectKey(image.getWatermarkedObjectKey())
                .originalFileName(image.getOriginalFileName())
                .contentType(image.getContentType())
                .fileSize(image.getFileSize())
                .width(image.getWidth())
                .height(image.getHeight())
                .etag(image.getEtag())
                .uploadedAt(image.getUploadedAt())
                .status(image.getStatus())
                .processingVersion(image.getProcessingVersion())
                .failureCode(image.getFailureCode())
                .sortOrder(image.getSortOrder())
                .thumbnail(image.isThumbnail())
                .build();
    }

    public void updateFrom(PromptImage image) {
        if (!id.equals(image.getPromptImageId().id())) {
            throw new IllegalArgumentException("다른 이미지 자산의 상태로 갱신할 수 없습니다.");
        }

        promptId = image.getPromptId();
        watermarkedObjectKey = image.getWatermarkedObjectKey();
        etag = image.getEtag();
        uploadedAt = image.getUploadedAt();
        status = image.getStatus();
        processingVersion = image.getProcessingVersion();
        failureCode = image.getFailureCode();
        sortOrder = image.getSortOrder();
        thumbnail = image.isThumbnail();
    }

    public PromptImage toDomain() {
        return PromptImage.reconstruct(
                new PromptImageId(id),
                uploaderId,
                promptId,
                originalObjectKey,
                watermarkedObjectKey,
                originalFileName,
                contentType,
                fileSize,
                width,
                height,
                status,
                etag,
                uploadedAt,
                processingVersion,
                failureCode,
                sortOrder,
                thumbnail,
                getCreatedAt(),
                getUpdatedAt()
        );
    }
}
