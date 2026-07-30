package com.promsearch.prompt.domain.enums;

import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import java.util.Arrays;
import java.util.Locale;

public enum PromptImageContentType {

    JPEG("image/jpeg"),
    PNG("image/png");

    private final String mimeType;

    PromptImageContentType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getMimeType() {
        return mimeType;
    }

    public static PromptImageContentType fromMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            throw new PromptDomainException(PromptErrorCode.UNSUPPORTED_IMAGE_CONTENT_TYPE);
        }

        String normalizedMimeType = mimeType.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(type -> type.mimeType.equals(normalizedMimeType))
                .findFirst()
                .orElseThrow(() -> new PromptDomainException(PromptErrorCode.UNSUPPORTED_IMAGE_CONTENT_TYPE));
    }
}
