package com.promsearch.commerce.application.usecase.dto;

public record CopyPromptInfo(
        Long promptId,
        long copyCount,
        boolean newlyCounted
) {
}
