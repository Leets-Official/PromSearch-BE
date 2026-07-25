package com.promsearch.prompt.interfaces.dto;

import com.promsearch.prompt.application.HomePromptSummaryInfo;
import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "홈 화면 프롬프트 카드 응답")
public record HomePromptSummaryResponse(
        @Schema(description = "프롬프트 ID", example = "10")
        Long promptId,

        @Schema(description = "카드 제목", example = "금융 앱 온보딩 UI")
        String title,

        @Schema(description = "카드 썸네일 이미지 URL", example = "https://cdn.promsearch.com/prompts/10/thumb.webp", nullable = true)
        String thumbnailImageUrl,

        @Schema(description = "결과물 타입", example = "IMAGE")
        PromptOutputType outputType,

        @Schema(description = "콘텐츠 타입. 홈에서는 FREE/PREMIUM만 노출합니다.", example = "PREMIUM")
        PromptContentType contentType,

        @Schema(description = "서버가 결정한 가격. FREE는 0입니다.", example = "500")
        Long pricePoint,

        @Schema(description = "작성자 카드 정보")
        HomePromptAuthorResponse author,

        @Schema(description = "카드 통계")
        HomePromptStatisticsResponse statistics,

        @Schema(description = "현재 로그인 사용자 기준 좋아요/북마크 상태")
        HomePromptViewerInteractionResponse viewerInteraction,

        @Schema(description = "카드에 표시할 태그 목록")
        List<HomePromptTagResponse> tags,

        @Schema(description = "게시 시각", example = "2026-07-23T12:00:00Z")
        Instant createdAt
) {

    public static HomePromptSummaryResponse from(HomePromptSummaryInfo info) {
        return new HomePromptSummaryResponse(
                info.promptId(),
                info.title(),
                info.thumbnailImageUrl(),
                info.outputType(),
                info.contentType(),
                info.pricePoint(),
                HomePromptAuthorResponse.from(info.author()),
                HomePromptStatisticsResponse.from(info.statistics()),
                HomePromptViewerInteractionResponse.from(info.viewerInteraction()),
                info.tags().stream()
                        .map(HomePromptTagResponse::from)
                        .toList(),
                info.createdAt()
        );
    }
}
