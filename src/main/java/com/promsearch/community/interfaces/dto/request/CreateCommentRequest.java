package com.promsearch.community.interfaces.dto.request;

import com.promsearch.community.application.usecase.dto.CreateCommentCommand;
import com.promsearch.community.application.usecase.dto.CreateCommentReplyCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "댓글 또는 대댓글 작성 요청")
public record CreateCommentRequest(
        @Schema(description = "댓글 내용", example = "좋은 프롬프트네요.")
        @NotBlank(message = "content must not be blank")
        String content
) {
    public CreateCommentCommand toCommentCommand(Long postId, Long userId) {
        return CreateCommentCommand.of(postId, userId, content);
    }

    public CreateCommentReplyCommand toReplyCommand(Long parentCommentId, Long userId) {
        return CreateCommentReplyCommand.of(parentCommentId, userId, content);
    }
}
