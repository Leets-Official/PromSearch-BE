package com.promsearch.prompt.interfaces.dto;

import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "프롬프트 상세 조회 응답")
public record PromptDetailResponse(
        @Schema(description = "프롬프트 ID", example = "10")
        Long promptId,

        @Schema(description = "제목", example = "먹음직스러운 파스타 사진 생성")
        String title,

        @Schema(description = "작성자 정보")
        PromptAuthorResponse author,

        @Schema(description = "출력 형식", example = "IMAGE")
        PromptOutputType outputType,

        @Schema(
                description = "콘텐츠 타입",
                example = "PREMIUM",
                allowableValues = {"FREE", "PREMIUM"}
        )
        PromptContentType contentType,

        @Schema(description = "서버에서 관리하는 콘텐츠 가격. FREE는 0입니다.", example = "500")
        Long pricePoint,

        @Schema(
                description = "권한에 따라 서버가 제한한 프롬프트 본문. "
                        + "비회원은 빈 문자열, PREMIUM 미결제 회원은 앞부분 10% 이내이면서 최대 200자입니다.",
                example = "cinematic food photography of pasta..."
        )
        String promptBody,

        @Schema(description = "프롬프트 설명", example = "파스타의 결감을 살린 이미지 생성 프롬프트입니다. 명도를 조절해 원하시는 이미지를 완성해보세요!", nullable = true)
        String description,

        @Schema(description = "요청 사용자 기준 본문 접근 상태")
        PromptAccessResponse access,

        @Schema(description = "요청 사용자 기준 좋아요 및 북마크 상태. 비로그인 사용자는 모두 false입니다.")
        PromptViewerInteractionResponse viewerInteraction,

        @Schema(description = "워터마크 처리가 완료된 결과 이미지 목록")
        List<PromptImageResponse> images,

        @Schema(description = "태그 목록")
        List<PromptTagResponse> tags,

        @Schema(description = "공개 통계")
        PromptStatisticsResponse statistics,

        @Schema(description = "생성 시각", example = "2026-07-23T01:30:00Z")
        Instant createdAt,

        @Schema(description = "수정 시각", example = "2026-07-23T02:00:00Z")
        Instant updatedAt
) {
}
