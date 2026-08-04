package com.promsearch.prompt.interfaces.dto.response;

import com.promsearch.prompt.application.usecase.dto.HomePromptSummaryInfo;
import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "홈 프롬프트 카드 응답")
public record HomePromptSummaryResponse(
        @Schema(description = "프롬프트 ID", example = "10")
        Long promptId,

        @Schema(description = "프롬프트 카드 제목", example = "금융 대시보드 UI 프롬프트")
        String title,

        @Schema(description = "프롬프트 카드 썸네일 이미지 URL", example = "https://cdn.promsearch.com/prompts/10/thumb.webp", nullable = true)
        String thumbnailImageUrl,

        @Schema(description = "결과물 타입", example = "IMAGE")
        PromptOutputType outputType,

        @Schema(description = "홈에 노출되는 콘텐츠 타입. FREE와 PREMIUM만 지원합니다.", example = "PREMIUM")
        PromptContentType contentType,

        @Schema(description = "서버가 결정한 가격 포인트. FREE 프롬프트는 0을 반환합니다.", example = "500")
        Long pricePoint,

        @Schema(description = "작성자 카드 정보")
        HomePromptAuthorResponse author,

        @Schema(description = "카드 공개 통계")
        HomePromptStatisticsResponse statistics,

        @Schema(
                description = "현재 조회자의 상호작용 상태. 비로그인 사용자에게도 null이 아닌 { liked: false, bookmarked: false } 객체를 반환합니다.",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        HomePromptViewerInteractionResponse viewerInteraction,

        @Schema(description = "카드에 표시할 태그 목록")
        List<HomePromptTagResponse> tags,

        @Schema(description = "사용자가 직접 입력한 AI 모델명 목록. 없으면 빈 배열입니다.", example = "[\"GPT 4.1 Mini\"]")
        List<String> customAiModels,

        @Schema(description = "생성 시각", example = "2026-07-23T12:00:00Z")
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
                info.customAiModels(),
                info.createdAt()
        );
    }
}
