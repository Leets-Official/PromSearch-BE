package com.promsearch.prompt.interfaces.dto;

import com.promsearch.prompt.application.HomePromptListInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "목록 페이지 정보")
public record HomePromptPageResponse(
        @Schema(description = "0부터 시작하는 페이지 번호", example = "0")
        int page,

        @Schema(description = "페이지 크기", example = "12")
        int size,

        @Schema(description = "전체 요소 수", example = "128")
        long totalElements,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext
) {

    public static HomePromptPageResponse from(HomePromptListInfo info) {
        return new HomePromptPageResponse(info.page(), info.size(), info.totalElements(), info.hasNext());
    }
}
