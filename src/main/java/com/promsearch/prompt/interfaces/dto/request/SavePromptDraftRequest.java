package com.promsearch.prompt.interfaces.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.promsearch.prompt.domain.Prompt;
import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import com.promsearch.prompt.domain.enums.PromptVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "내 최신 임시저장 생성 또는 교체 요청. 제목 외 필드는 생략할 수 있습니다.")
public record SavePromptDraftRequest(
        @Schema(description = "제목. 공백이 아닌 문자가 한 글자 이상 있어야 합니다.", example = "회의록 자동 정리", maxLength = Prompt.MAX_TITLE_LENGTH)
        @NotBlank(message = "title must not be blank")
        @Size(max = Prompt.MAX_TITLE_LENGTH, message = "title must be 500 characters or less")
        String title,

        @Schema(description = "프롬프트 설명. 별도 최대 글자 수 제한이 없는 TEXT 값입니다.")
        String description,

        @Schema(description = "프롬프트 실행 결과 타입", example = "TEXT")
        PromptOutputType outputType,

        @Schema(description = "선택한 직군 태그 식별자 목록", example = "[1, 2]")
        List<Long> jobTagIds,

        @Schema(description = "선택한 태스크 태그 식별자 목록", example = "[10, 11]")
        List<Long> taskTagIds,

        @Schema(description = "선택한 AI 모델 태그 식별자 목록", example = "[20]")
        List<Long> aiModelTagIds,

        @Schema(description = "AI 모델 '기타' 직접 입력값. 서버에서 소문자 변환 및 공백 제거 후 검색용으로 저장합니다.", example = "GPT 4.1 Mini")
        String customAiModel,

        @Schema(description = "콘텐츠 타입", example = "FREE")
        PromptContentType contentType,

        @Schema(description = "프롬프트 본문. 별도 최대 글자 수 제한이 없는 TEXT 값입니다.")
        String promptBody,

        @Schema(description = "작성자 공개 범위. 미입력 시 PUBLIC입니다.", example = "PUBLIC", defaultValue = "PUBLIC")
        PromptVisibility visibility,

        @Schema(description = "업로드 완료 이미지. 최대 10장이며 imageId로만 연결합니다.")
        @Size(max = 10, message = "images must contain 10 items or less")
        List<@Valid PromptImageRequest> images
) {

    public SavePromptDraftRequest {
        title = title == null ? null : title.strip();
        visibility = visibility == null ? PromptVisibility.PUBLIC : visibility;
    }

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "thumbnail can be selected for at most one image")
    public boolean hasAtMostOneThumbnail() {
        return images == null || images.stream().filter(PromptImageRequest::thumbnail).limit(2).count() <= 1;
    }
}
