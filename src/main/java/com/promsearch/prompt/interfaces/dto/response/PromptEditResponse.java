package com.promsearch.prompt.interfaces.dto.response;

import com.promsearch.prompt.application.usecase.dto.PromptEditInfo;
import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.domain.enums.PromptVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "작성자 전용 프롬프트 수정 폼 데이터")
public record PromptEditResponse(
        Long promptId,
        String title,
        String description,
        PromptOutputType outputType,
        List<Long> jobTagIds,
        List<Long> taskTagIds,
        List<Long> aiModelTagIds,
        String customAiModel,
        PromptContentType contentType,
        String promptBody,
        PromptVisibility visibility,
        List<ImageResponse> images,
        PromptStatus status,
        Long pricePoint,
        Instant updatedAt
) {
    public static PromptEditResponse from(PromptEditInfo info) {
        return new PromptEditResponse(
                info.promptId(), info.title(), info.description(), info.outputType(),
                info.jobTagIds(), info.taskTagIds(), info.aiModelTagIds(), info.customAiModel(),
                info.contentType(), info.promptBody(), info.visibility(),
                info.images().stream().map(image -> new ImageResponse(
                        image.imageId(), image.imageUrl(), image.sortOrder(), image.thumbnail())).toList(),
                info.status(), info.pricePoint(), info.updatedAt());
    }

    public record ImageResponse(UUID imageId, String imageUrl, int sortOrder, boolean thumbnail) {
    }
}
