package com.promsearch.moderation.interfaces;

import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.response.code.SuccessCode;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import com.promsearch.moderation.application.usecase.CreateCommentReportUseCase;
import com.promsearch.moderation.application.usecase.CreatePostReportUseCase;
import com.promsearch.moderation.application.usecase.dto.CreateCommentReportCommand;
import com.promsearch.moderation.application.usecase.dto.CreatePostReportCommand;
import com.promsearch.moderation.interfaces.docs.ReportControllerDocs;
import com.promsearch.moderation.interfaces.dto.request.CreateReportRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reports")
public class ReportController implements ReportControllerDocs {

    private final CreatePostReportUseCase createPostReportUseCase;
    private final CreateCommentReportUseCase createCommentReportUseCase;

    @Override
    @PostMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<Void>> reportPost(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @Positive @PathVariable Long postId,
            @Valid @RequestBody CreateReportRequest request
    ) {
        createPostReportUseCase.create(
                new CreatePostReportCommand(user.userId(), postId, request.reason(), request.description()));
        return created();
    }

    @Override
    @PostMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> reportComment(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @Positive @PathVariable Long commentId,
            @Valid @RequestBody CreateReportRequest request
    ) {
        createCommentReportUseCase.create(
                new CreateCommentReportCommand(user.userId(), commentId, request.reason(), request.description()));
        return created();
    }

    private ResponseEntity<ApiResponse<Void>> created() {
        return ResponseEntity.status(SuccessCode.REPORT_ACCEPTED.getHttpStatus())
                .body(ApiResponse.onSuccess(SuccessCode.REPORT_ACCEPTED, null));
    }
}
