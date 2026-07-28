package com.promsearch.community.interfaces.dto.request;

import com.promsearch.community.application.usecase.dto.UpdateCommentCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "댓글 수정 요청")
public record UpdateCommentRequest(
        @Schema(description = "수정할 댓글 내용", example = "수정된 댓글 내용입니다.")
        @NotBlank(message = "content must not be blank")
        @Size(max = 500, message = "content must be 500 characters or less")
        String content
) {
    public UpdateCommentCommand toCommand(Long commentId, Long userId) {
        return UpdateCommentCommand.of(commentId, userId, content);
    }
}
