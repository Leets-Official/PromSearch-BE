package com.promsearch.prompt.application.usecase.dto;

import java.util.List;

public record IssuePromptImageUploadUrlsCommand(
        Long uploaderId,
        List<ImageFile> images
) {

    public record ImageFile(
            String fileName,
            String contentType,
            long fileSize,
            int width,
            int height
    ) {
    }
}
