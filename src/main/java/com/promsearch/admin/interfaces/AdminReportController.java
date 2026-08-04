package com.promsearch.admin.interfaces;

import com.promsearch.admin.interfaces.docs.AdminReportControllerDocs;
import com.promsearch.admin.interfaces.dto.request.UpdateReportStatusRequest;
import com.promsearch.admin.interfaces.dto.response.ReportSummaryResponse;
import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.response.PageResponse;
import com.promsearch.moderation.application.usecase.SearchReportsUseCase;
import com.promsearch.moderation.application.usecase.UpdateReportStatusUseCase;
import com.promsearch.moderation.application.usecase.dto.ReportPageInfo;
import com.promsearch.moderation.application.usecase.dto.SearchReportsQuery;
import com.promsearch.moderation.domain.enums.ReportStatus;
import com.promsearch.moderation.domain.enums.ReportTargetType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/admin/reports")
public class AdminReportController implements AdminReportControllerDocs {

    private final SearchReportsUseCase searchReportsUseCase;
    private final UpdateReportStatusUseCase updateReportStatusUseCase;

    @GetMapping
    @Override
    public ApiResponse<PageResponse<ReportSummaryResponse>> getReports(
            @RequestParam ReportTargetType targetType,
            @RequestParam(required = false) ReportStatus status,
            @Min(value = 0, message = "page must be 0 or greater")
            @RequestParam(defaultValue = "0") int page,
            @Min(value = 1, message = "size must be 1 or greater")
            @Max(value = 100, message = "size must be 100 or less")
            @RequestParam(defaultValue = "20") int size
    ) {
        ReportPageInfo pageInfo = searchReportsUseCase.searchReports(
                SearchReportsQuery.of(targetType, status, page, size)
        );
        List<ReportSummaryResponse> content = pageInfo.content().stream()
                .map(ReportSummaryResponse::from)
                .toList();
        return ApiResponse.onSuccess(PageResponse.of(content, page, size, pageInfo.totalElements()));
    }

    @PatchMapping("/{reportId}")
    @Override
    public ApiResponse<ReportSummaryResponse> updateReportStatus(
            @Positive(message = "reportId must be greater than 0") @PathVariable Long reportId,
            @Valid @RequestBody UpdateReportStatusRequest request
    ) {
        return ApiResponse.onSuccess(
                ReportSummaryResponse.from(updateReportStatusUseCase.updateStatus(request.toCommand(reportId)))
        );
    }
}
