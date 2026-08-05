package com.promsearch.prompt.interfaces.dto.response;

import com.promsearch.prompt.application.usecase.dto.HomePromptListInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "홈 프롬프트 카드 목록 응답")
public record HomePromptListResponse(
        @Schema(description = "프롬프트 카드 목록")
        List<HomePromptSummaryResponse> prompts,

        @Schema(description = "페이지 정보")
        HomePromptPageResponse page
) {

    public static HomePromptListResponse from(HomePromptListInfo info) {
        return new HomePromptListResponse(
                info.prompts().stream()
                        .map(HomePromptSummaryResponse::from)
                        .toList(),
                HomePromptPageResponse.from(info)
        );
    }
}
