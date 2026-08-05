package com.promsearch.prompt.application.usecase.dto;

import java.util.List;

public record PromptImageStatusesInfo(
        List<PromptImageStatusInfo> images
) {

    public PromptImageStatusesInfo {
        images = List.copyOf(images);
    }
}
