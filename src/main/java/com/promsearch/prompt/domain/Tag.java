package com.promsearch.prompt.domain;

import com.promsearch.prompt.domain.enums.TagType;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class Tag {

    public static final int MAX_CUSTOM_AI_MODEL_LENGTH = 50;

    private final TagId tagId;
    private final TagType tagType;
    private final String tagName;
    private final String normalizedName;
    private final boolean custom;

    @Builder(access = AccessLevel.PRIVATE)
    private Tag(TagId tagId, TagType tagType, String tagName, String normalizedName, boolean custom) {
        this.tagId = tagId;
        this.tagType = tagType;
        this.tagName = tagName;
        this.normalizedName = normalizedName;
        this.custom = custom;
    }

    public static Tag create(TagType tagType, String tagName, String normalizedName, boolean custom) {
        validateRequired(tagType, tagName);

        return Tag.builder()
                .tagType(tagType)
                .tagName(tagName)
                .normalizedName(normalizedName)
                .custom(custom)
                .build();
    }

    public static Tag createCustomAiModel(String tagName) {
        if (tagName == null || tagName.strip().length() > MAX_CUSTOM_AI_MODEL_LENGTH) {
            throw new PromptDomainException(PromptErrorCode.INVALID_TAG_NAME);
        }
        String normalizedName = normalizeAiModelName(tagName);
        return create(TagType.AI_MODEL, tagName.strip(), normalizedName, true);
    }

    public static String normalizeAiModelName(String tagName) {
        if (tagName == null || tagName.isBlank()) {
            throw new PromptDomainException(PromptErrorCode.INVALID_TAG_NAME);
        }

        String normalizedName = tagName.codePoints()
                .filter(codePoint -> !Character.isWhitespace(codePoint))
                .collect(
                        StringBuilder::new,
                        StringBuilder::appendCodePoint,
                        StringBuilder::append
                )
                .toString()
                .toLowerCase(Locale.ROOT);
        if (normalizedName.isBlank()) {
            throw new PromptDomainException(PromptErrorCode.INVALID_TAG_NAME);
        }
        return normalizedName;
    }

    public static Tag reconstruct(
            TagId tagId,
            TagType tagType,
            String tagName,
            String normalizedName,
            boolean custom
    ) {
        validateRequired(tagType, tagName);

        return Tag.builder()
                .tagId(tagId)
                .tagType(tagType)
                .tagName(tagName)
                .normalizedName(normalizedName)
                .custom(custom)
                .build();
    }

    private static void validateRequired(TagType tagType, String tagName) {
        if (tagType == null) {
            throw new PromptDomainException(PromptErrorCode.INVALID_TAG_TYPE);
        }
        if (tagName == null || tagName.isBlank()) {
            throw new PromptDomainException(PromptErrorCode.INVALID_TAG_NAME);
        }
        if (tagName.length() > 100) {
            throw new PromptDomainException(PromptErrorCode.INVALID_TAG_NAME);
        }
    }

    public record TagId(Long id) {
        public TagId {
            if (id == null || id <= 0) {
                throw new PromptDomainException(PromptErrorCode.INVALID_TAG_ID);
            }
        }
    }
}
