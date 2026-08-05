package com.promsearch.prompt.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프롬프트 작성자 정보")
public record PromptAuthorResponse(
        @Schema(description = "작성자 ID", example = "12")
        Long userId,

        @Schema(description = "작성자 닉네임", example = "프롬프트장인")
        String nickname,

        @Schema(
                description = "작성자 프로필 이미지 URL. 등록되지 않았으면 null입니다.",
                example = "https://cdn.promsearch.com/profiles/12.jpg",
                nullable = true
        )
        String profileImageUrl,

        @Schema(description = "작성자 크리에이터 등급 이름", example = "ORIGIN")
        String gradeName
) {
}
