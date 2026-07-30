package com.promsearch.prompt.application.usecase.dto;

public record HomePromptViewerInteractionInfo(
        boolean liked,
        boolean bookmarked
) {

    public static HomePromptViewerInteractionInfo none() {
        return new HomePromptViewerInteractionInfo(false, false);
    }
}
