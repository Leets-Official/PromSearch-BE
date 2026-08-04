package com.promsearch.moderation.interfaces.docs;

import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import com.promsearch.moderation.interfaces.dto.request.CreateReportRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Moderation | 신고", description = "게시글 및 댓글 신고 API")
@SecurityRequirement(name = "jwtBearerAuth")
public interface ReportControllerDocs {

    String IMPLEMENTATION_METADATA = "**작업자: 이건희 | 구현 상태: 구현완료**\n\n";

    @Operation(summary = "[MODERATION-001] 게시글 신고", description = IMPLEMENTATION_METADATA
            + "JWT 인증 사용자가 존재하는 게시글을 신고 사유와 상세 설명으로 신고합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "신고 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "중복 신고")
    })
    ResponseEntity<ApiResponse<Void>> reportPost(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @Positive @PathVariable Long postId,
            @Valid @RequestBody CreateReportRequest request
    );

    @Operation(summary = "[MODERATION-002] 댓글 신고", description = IMPLEMENTATION_METADATA
            + "JWT 인증 사용자가 존재하는 댓글을 신고 사유와 상세 설명으로 신고합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "신고 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "중복 신고")
    })
    ResponseEntity<ApiResponse<Void>> reportComment(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @Positive @PathVariable Long commentId,
            @Valid @RequestBody CreateReportRequest request
    );
}
