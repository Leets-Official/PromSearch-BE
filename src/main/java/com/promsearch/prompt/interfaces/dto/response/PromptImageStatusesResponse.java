package com.promsearch.prompt.interfaces.dto.response;

import com.promsearch.prompt.application.usecase.dto.PromptImageStatusesInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "프롬프트 이미지 처리 상태 일괄 조회 결과")
public record PromptImageStatusesResponse(
        @Schema(description = "요청 순서대로 정렬된 이미지 처리 상태 목록")
        List<PromptImageStatusResponse> images
) {

    public static PromptImageStatusesResponse from(PromptImageStatusesInfo info) {
        return new PromptImageStatusesResponse(info.images().stream()
                .map(PromptImageStatusResponse::from)
                .toList());
    }
}
