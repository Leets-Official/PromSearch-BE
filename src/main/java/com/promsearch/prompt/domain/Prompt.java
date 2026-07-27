package com.promsearch.prompt.domain;

import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.domain.enums.PromptVisibility;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class Prompt {

    private final PromptId promptId;
    private final Long userId;
    private final String title;
    private final String promptBody;
    private final String thumbnailImage;
    private final PromptOutputType outputType;
    private final String description;
    private final PromptContentType contentType;
    private final PromptStatus status;
    private final PromptVisibility visibility;
    private final Long pricePoint;
    private final String hiddenReason;
    private final Instant hiddenAt;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant deletedAt;
    private final List<PromptImage> images;
    private final PostStatistics statistics;
    private final List<PostTag> postTags;

    @Builder(access = AccessLevel.PRIVATE)
    private Prompt(
            PromptId promptId,
            Long userId,
            String title,
            String promptBody,
            String thumbnailImage,
            PromptOutputType outputType,
            String description,
            PromptContentType contentType,
            PromptStatus status,
            PromptVisibility visibility,
            Long pricePoint,
            String hiddenReason,
            Instant hiddenAt,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt,
            List<PromptImage> images,
            PostStatistics statistics,
            List<PostTag> postTags
    ) {
        this.promptId = promptId;
        this.userId = userId;
        this.title = title;
        this.promptBody = promptBody;
        this.thumbnailImage = thumbnailImage;
        this.outputType = outputType;
        this.description = description;
        this.contentType = contentType;
        this.status = status;
        this.visibility = visibility;
        this.pricePoint = pricePoint;
        this.hiddenReason = hiddenReason;
        this.hiddenAt = hiddenAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.images = images == null ? List.of() : List.copyOf(images);
        this.statistics = statistics;
        this.postTags = postTags == null ? List.of() : List.copyOf(postTags);
    }

    public static Prompt create(
            Long userId,
            String title,
            String promptBody,
            String thumbnailImage,
            PromptOutputType outputType,
            String description,
            PromptContentType contentType,
            Long pricePoint
    ) {
        validateRequired(userId, title, outputType, contentType, pricePoint);

        Instant now = Instant.now();
        return Prompt.builder()
                .userId(userId)
                .title(title)
                .promptBody(promptBody)
                .thumbnailImage(thumbnailImage)
                .outputType(outputType)
                .description(description)
                .contentType(contentType)
                .status(PromptStatus.DRAFT)
                .visibility(PromptVisibility.PUBLIC)
                .pricePoint(pricePoint)
                .createdAt(now)
                .updatedAt(now)
                .images(List.of())
                .postTags(List.of())
                .build();
    }

    public static Prompt createActive(
            Long userId,
            String title,
            String promptBody,
            PromptOutputType outputType,
            String description,
            PromptContentType contentType,
            PromptVisibility visibility,
            Long pricePoint
    ) {
        validateRequired(userId, title, outputType, contentType, pricePoint);
        validatePromptBody(promptBody);
        validateDescription(description);
        if (visibility == null) {
            throw new PromptDomainException(PromptErrorCode.INVALID_PROMPT_VISIBILITY);
        }
        validatePricePolicy(contentType, pricePoint);

        Instant now = Instant.now();
        return Prompt.builder()
                .userId(userId)
                .title(title.strip())
                .promptBody(promptBody)
                .outputType(outputType)
                .description(description)
                .contentType(contentType)
                .status(PromptStatus.ACTIVE)
                .visibility(visibility)
                .pricePoint(pricePoint)
                .createdAt(now)
                .updatedAt(now)
                .images(List.of())
                .postTags(List.of())
                .build();
    }

    public static Prompt reconstruct(
            PromptId promptId,
            Long userId,
            String title,
            String promptBody,
            String thumbnailImage,
            PromptOutputType outputType,
            String description,
            PromptContentType contentType,
            PromptStatus status,
            PromptVisibility visibility,
            Long pricePoint,
            String hiddenReason,
            Instant hiddenAt,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt,
            List<PromptImage> images,
            PostStatistics statistics,
            List<PostTag> postTags
    ) {
        if (status == null) {
            throw new PromptDomainException(PromptErrorCode.INVALID_PROMPT_STATUS);
        }
        validateRequired(userId, title, outputType, contentType, pricePoint);
        if (visibility == null) {
            throw new PromptDomainException(PromptErrorCode.INVALID_PROMPT_VISIBILITY);
        }
        if (status == PromptStatus.ACTIVE) {
            validatePromptBody(promptBody);
            validateDescription(description);
            validatePricePolicy(contentType, pricePoint);
        }

        return Prompt.builder()
                .promptId(promptId)
                .userId(userId)
                .title(title)
                .promptBody(promptBody)
                .thumbnailImage(thumbnailImage)
                .outputType(outputType)
                .description(description)
                .contentType(contentType)
                .status(status)
                .visibility(visibility)
                .pricePoint(pricePoint)
                .hiddenReason(hiddenReason)
                .hiddenAt(hiddenAt)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .deletedAt(deletedAt)
                .images(images)
                .statistics(statistics)
                .postTags(postTags)
                .build();
    }

    private static void validateRequired(
            Long userId,
            String title,
            PromptOutputType outputType,
            PromptContentType contentType,
            Long pricePoint
    ) {
        if (userId == null || userId <= 0) {
            throw new PromptDomainException(PromptErrorCode.INVALID_PROMPT_USER_ID);
        }
        if (title == null || title.isBlank() || title.strip().length() > 20) {
            throw new PromptDomainException(PromptErrorCode.INVALID_PROMPT_TITLE);
        }
        if (outputType == null) {
            throw new PromptDomainException(PromptErrorCode.INVALID_OUTPUT_TYPE);
        }
        if (contentType == null) {
            throw new PromptDomainException(PromptErrorCode.INVALID_CONTENT_TYPE);
        }
        if (pricePoint == null || pricePoint < 0) {
            throw new PromptDomainException(PromptErrorCode.INVALID_PRICE_POINT);
        }
    }

    private static void validatePromptBody(String promptBody) {
        if (promptBody == null || promptBody.isBlank()) {
            throw new PromptDomainException(PromptErrorCode.INVALID_PROMPT_BODY);
        }
    }

    private static void validateDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new PromptDomainException(PromptErrorCode.INVALID_PROMPT_DESCRIPTION);
        }
    }

    private static void validatePricePolicy(PromptContentType contentType, Long pricePoint) {
        if ((contentType == PromptContentType.FREE && pricePoint != 0)
                || (contentType == PromptContentType.PREMIUM && pricePoint <= 0)) {
            throw new PromptDomainException(PromptErrorCode.INVALID_PRICE_POINT);
        }
    }

    public record PromptId(Long id) {
        public PromptId {
            if (id == null || id <= 0) {
                throw new PromptDomainException(PromptErrorCode.INVALID_ID);
            }
        }
    }
}
