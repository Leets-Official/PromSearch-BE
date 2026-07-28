package com.promsearch.community.interfaces;

import com.promsearch.community.application.usecase.CreateCommentReplyUseCase;
import com.promsearch.community.application.usecase.CreateCommentUseCase;
import com.promsearch.community.application.usecase.DeleteCommentUseCase;
import com.promsearch.community.application.usecase.GetCommentsUseCase;
import com.promsearch.community.application.usecase.UpdateCommentUseCase;
import com.promsearch.community.application.usecase.dto.DeleteCommentCommand;
import com.promsearch.community.application.usecase.dto.GetCommentsQuery;
import com.promsearch.community.interfaces.docs.CommentControllerDocs;
import com.promsearch.community.interfaces.dto.request.CreateCommentRequest;
import com.promsearch.community.interfaces.dto.request.UpdateCommentRequest;
import com.promsearch.community.interfaces.dto.response.CommentListResponse;
import com.promsearch.community.interfaces.dto.response.CommentReplyResponse;
import com.promsearch.community.interfaces.dto.response.CommentResponse;
import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.response.code.SuccessCode;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class CommentController implements CommentControllerDocs {

    private final GetCommentsUseCase getCommentsUseCase;
    private final CreateCommentUseCase createCommentUseCase;
    private final UpdateCommentUseCase updateCommentUseCase;
    private final DeleteCommentUseCase deleteCommentUseCase;
    private final CreateCommentReplyUseCase createCommentReplyUseCase;

    @GetMapping("/prompts/{promptId}/comments")
    @Override
    public ApiResponse<CommentListResponse> getComments(
            @PathVariable Long promptId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal user
    ) {
        Long viewerId = user == null ? null : user.userId();
        return ApiResponse.onSuccess(CommentListResponse.from(
                getCommentsUseCase.getComments(GetCommentsQuery.of(promptId, viewerId))
        ));
    }

    @PostMapping("/prompts/{promptId}/comments")
    @Override
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @PathVariable Long promptId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @RequestBody CreateCommentRequest request
    ) {
        ApiResponse<CommentResponse> response = ApiResponse.onSuccess(
                SuccessCode.CREATED,
                CommentResponse.from(createCommentUseCase.createComment(
                        request.toCommentCommand(promptId, user.userId())
                ))
        );
        return ResponseEntity.status(SuccessCode.CREATED.getHttpStatus()).body(response);
    }

    @PatchMapping("/comments/{commentId}")
    @Override
    public ApiResponse<CommentResponse> updateComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @RequestBody UpdateCommentRequest request
    ) {
        return ApiResponse.onSuccess(CommentResponse.from(
                updateCommentUseCase.updateComment(request.toCommand(commentId, user.userId()))
        ));
    }

    @DeleteMapping("/comments/{commentId}")
    @Override
    public ApiResponse<Void> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal user
    ) {
        deleteCommentUseCase.deleteComment(DeleteCommentCommand.of(commentId, user.userId()));
        return ApiResponse.onSuccess(null);
    }

    @PostMapping("/comments/{commentId}/replies")
    @Override
    public ResponseEntity<ApiResponse<CommentReplyResponse>> createReply(
            @PathVariable Long commentId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @RequestBody CreateCommentRequest request
    ) {
        ApiResponse<CommentReplyResponse> response = ApiResponse.onSuccess(
                SuccessCode.CREATED,
                CommentReplyResponse.from(createCommentReplyUseCase.createReply(
                        request.toReplyCommand(commentId, user.userId())
                ))
        );
        return ResponseEntity.status(SuccessCode.CREATED.getHttpStatus()).body(response);
    }
}
