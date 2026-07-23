package com.promsearch.community.interfaces;

import com.promsearch.community.interfaces.docs.CommentControllerDocs;
import com.promsearch.community.interfaces.dto.CommentListResponse;
import com.promsearch.community.interfaces.dto.CommentReplyResponse;
import com.promsearch.community.interfaces.dto.CommentResponse;
import com.promsearch.community.interfaces.dto.CreateCommentRequest;
import com.promsearch.community.interfaces.dto.UpdateCommentRequest;
import com.promsearch.global.exception.NotImplementedException;
import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1")
public class CommentController implements CommentControllerDocs {

    @GetMapping("/prompts/{promptId}/comments")
    @Override
    public ApiResponse<CommentListResponse> getComments(
            @PathVariable Long promptId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal user
    ) {
        throw new NotImplementedException("댓글 목록 조회 기능은 아직 구현되지 않았습니다.");
    }

    @PostMapping("/prompts/{promptId}/comments")
    @Override
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @PathVariable Long promptId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        throw new NotImplementedException("댓글 작성 기능은 아직 구현되지 않았습니다.");
    }

    @PatchMapping("/comments/{commentId}")
    @Override
    public ApiResponse<CommentResponse> updateComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @Valid @RequestBody UpdateCommentRequest request
    ) {
        throw new NotImplementedException("댓글 수정 기능은 아직 구현되지 않았습니다.");
    }

    @DeleteMapping("/comments/{commentId}")
    @Override
    public ApiResponse<Void> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal user
    ) {
        throw new NotImplementedException("댓글 삭제 기능은 아직 구현되지 않았습니다.");
    }

    @PostMapping("/comments/{commentId}/replies")
    @Override
    public ResponseEntity<ApiResponse<CommentReplyResponse>> createReply(
            @PathVariable Long commentId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        throw new NotImplementedException("대댓글 작성 기능은 아직 구현되지 않았습니다.");
    }
}
