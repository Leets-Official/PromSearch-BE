package com.promsearch.community.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "댓글 작성자 공개 정보")
public record CommentAuthorResponse(
        @Schema(description = "작성자 ID", example = "8")
        Long userId,

        @Schema(description = "작성자 닉네임", example = "홍길동")
        String nickname,

        @Schema(
                description = "작성자 프로필 이미지 URL",
                example = "https://cdn.promsearch.com/profiles/8.jpg",
                nullable = true
        )
        String profileImageUrl
) {
}
