package com.promsearch.prompt.interfaces.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.promsearch.prompt.application.usecase.dto.CreatePromptCommand;
import com.promsearch.prompt.application.usecase.dto.CreatePromptCommand.ImageReference;
import com.promsearch.prompt.domain.Prompt;
import com.promsearch.prompt.domain.Tag;
import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import com.promsearch.prompt.domain.enums.PromptVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Objects;

@Schema(description = "프롬프트 게시물 생성 요청.")
public record CreatePromptRequest(
        @Schema(description = "제목", example = "금융 앱 온보딩 UI", maxLength = Prompt.MAX_TITLE_LENGTH)
        @NotBlank(message = "title must not be blank")
        @Size(max = Prompt.MAX_TITLE_LENGTH, message = "title must be 500 characters or less")
        String title,

        @Schema(description = "프롬프트 설명. 필수이며 별도 최대 글자 수 제한이 없는 TEXT 값입니다.", example = "신규 사용자를 위한 금융 앱 온보딩 화면을 설계합니다.")
        @NotBlank(message = "description must not be blank")
        String description,

        @Schema(description = "프롬프트 실행 결과 타입", example = "IMAGE")
        @NotNull(message = "outputType must not be null")
        PromptOutputType outputType,

        @Schema(description = "선택한 직군 태그 식별자 목록", example = "[1, 2]")
        @NotNull(message = "jobTagIds must not be null")
        @Size(min = 1, message = "jobTagIds must contain at least one item")
        List<
                @NotNull(message = "jobTagIds must not contain null")
                @Positive(message = "jobTagIds must contain positive values") Long> jobTagIds,

        @Schema(description = "선택한 태스크 태그 식별자 목록", example = "[10, 11]")
        @NotNull(message = "taskTagIds must not be null")
        @Size(min = 1, message = "taskTagIds must contain at least one item")
        List<
                @NotNull(message = "taskTagIds must not contain null")
                @Positive(message = "taskTagIds must contain positive values") Long> taskTagIds,

        @Schema(description = "선택한 AI 모델 태그 식별자. customAiModel과 정확히 하나를 입력해야 합니다.", example = "20")
        @Positive(message = "aiModelTagId must be positive")
        Long aiModelTagId,

        @Schema(description = "AI 모델 '기타' 직접 입력값. 서버에서 소문자 변환 및 공백 제거 후 검색용으로 저장합니다.", example = "Flux 1.1 Pro")
        @Size(max = Tag.MAX_CUSTOM_AI_MODEL_LENGTH, message = "customAiModel must be 50 characters or less")
        String customAiModel,

        @Schema(description = "콘텐츠 타입. PREMIUM 가격은 서버 고정 설정값을 사용합니다.", example = "FREE")
        @NotNull(message = "contentType must not be null")
        PromptContentType contentType,

        @Schema(description = "프롬프트 본문. 별도 최대 글자 수 제한이 없는 TEXT 값입니다.", example = "20대 사용자를 위한 금융 앱 온보딩 화면 3개를 디자인해줘. 신뢰감 있는 블루 톤과 간결한 아이콘을 사용하고, 각 화면에 짧은 안내 문구와 다음 버튼을 포함해줘.")
        @NotBlank(message = "promptBody must not be blank")
        String promptBody,

        @Schema(description = "작성자 공개 범위. 필수이며 PUBLIC 또는 PRIVATE입니다.", example = "PUBLIC")
        @NotNull(message = "visibility must not be null")
        PromptVisibility visibility,

        @Schema(description = "업로드 완료 이미지. 최소 1장, 최대 10장이며 imageId로만 연결합니다.")
        @NotNull(message = "images must not be null")
        @Size(min = 1, max = 10, message = "images must contain between 1 and 10 items")
        List<@NotNull(message = "images must not contain null") @Valid PromptImageRequest> images
) {

    public CreatePromptRequest {
        title = title == null ? null : title.strip();
    }

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "exactly one AI model tag or customAiModel is required")
    public boolean hasAiModelSelection() {
        return (aiModelTagId != null) != (customAiModel != null && !customAiModel.isBlank());
    }

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "thumbnail can be selected for at most one image")
    public boolean hasAtMostOneThumbnail() {
        return images == null
                || images.stream()
                .filter(Objects::nonNull)
                .filter(PromptImageRequest::thumbnail)
                .limit(2)
                .count() <= 1;
    }

    public CreatePromptCommand toCommand(Long userId) {
        return new CreatePromptCommand(
                userId,
                title,
                description,
                outputType,
                jobTagIds,
                taskTagIds,
                aiModelTagId,
                customAiModel,
                contentType,
                promptBody,
                visibility,
                images == null
                        ? List.of()
                        : images.stream()
                                .map(image -> new ImageReference(
                                        image.imageId(),
                                        image.sortOrder(),
                                        image.thumbnail()
                                ))
                                .toList()
        );
    }
}
