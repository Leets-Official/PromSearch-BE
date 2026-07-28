package com.promsearch.admin.interfaces.docs;

import com.promsearch.admin.interfaces.dto.request.UpdateReportStatusRequest;
import com.promsearch.admin.interfaces.dto.response.ReportSummaryResponse;
import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.response.PageResponse;
import com.promsearch.moderation.domain.enums.ReportStatus;
import com.promsearch.moderation.domain.enums.ReportTargetType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Admin | 신고함", description = "신고 목록 조회 및 처리 API | 신고 생성 API는 이번 범위에서 미구현")
public interface AdminReportControllerDocs {

    @Operation(
            summary = "[ADMIN-REPORT-001] 신고 목록 조회",
            description = "신고 대상 타입(POST, COMMENT)과 처리 상태로 필터링해 신고 목록을 페이지네이션 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신고 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요")
    })
    ApiResponse<PageResponse<ReportSummaryResponse>> getReports(
            @Parameter(description = "신고 대상 타입 필터", example = "POST")
            @RequestParam(required = false) ReportTargetType targetType,

            @Parameter(description = "처리 상태 필터", example = "PENDING")
            @RequestParam(required = false) ReportStatus status,

            @Parameter(description = "페이지 번호(0부터 시작)", example = "0")
            @Min(value = 0, message = "page must be 0 or greater")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "페이지당 항목 수", example = "20")
            @Min(value = 1, message = "size must be 1 or greater")
            @Max(value = 100, message = "size must be 100 or less")
            @RequestParam(defaultValue = "20") int size
    );

    @Operation(
            summary = "[ADMIN-REPORT-002] 신고 처리 상태 변경",
            description = "신고를 RESOLVED 또는 REJECTED 상태로 변경합니다. PENDING으로는 되돌릴 수 없습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신고 처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "신고 없음")
    })
    ApiResponse<ReportSummaryResponse> updateReportStatus(
            @Parameter(description = "처리할 신고 식별자", example = "1")
            @Positive(message = "reportId must be greater than 0") @PathVariable Long reportId,

            @Valid @RequestBody UpdateReportStatusRequest request
    );
}
