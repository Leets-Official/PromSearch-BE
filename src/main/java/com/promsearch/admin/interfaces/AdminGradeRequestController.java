package com.promsearch.admin.interfaces;

import com.promsearch.admin.interfaces.docs.AdminGradeRequestControllerDocs;
import com.promsearch.admin.interfaces.dto.request.ProcessGradeRequestRequest;
import com.promsearch.admin.interfaces.dto.response.GradeRequestSummaryResponse;
import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.response.PageResponse;
import com.promsearch.user.application.usecase.ListGradeRequestsUseCase;
import com.promsearch.user.application.usecase.ProcessGradeRequestUseCase;
import com.promsearch.user.application.usecase.dto.GradeRequestListInfo;
import com.promsearch.user.application.usecase.dto.GradeRequestListQuery;
import com.promsearch.user.application.usecase.dto.GradeRequestSummaryInfo;
import com.promsearch.user.application.usecase.dto.ProcessGradeRequestCommand;
import com.promsearch.user.domain.enums.GradeRequestStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/v1/admin/grade-requests")
public class AdminGradeRequestController implements AdminGradeRequestControllerDocs {

    private final ListGradeRequestsUseCase listGradeRequestsUseCase;
    private final ProcessGradeRequestUseCase processGradeRequestUseCase;

    @GetMapping
    @Override
    public ApiResponse<PageResponse<GradeRequestSummaryResponse>> getGradeRequests(
            @RequestParam(required = false) GradeRequestStatus status,
            @Min(value = 0, message = "page must be 0 or greater")
            @RequestParam(defaultValue = "0") int page,
            @Min(value = 1, message = "size must be 1 or greater")
            @Max(value = 100, message = "size must be 100 or less")
            @RequestParam(defaultValue = "20") int size
    ) {
        GradeRequestListInfo info = listGradeRequestsUseCase.list(new GradeRequestListQuery(status, page, size));
        PageResponse<GradeRequestSummaryResponse> response = PageResponse.of(
                info.content().stream().map(GradeRequestSummaryResponse::from).toList(),
                info.page(),
                info.size(),
                info.totalElements()
        );
        return ApiResponse.onSuccess(response);
    }

    @PatchMapping("/{requestId}")
    @Override
    public ApiResponse<GradeRequestSummaryResponse> processGradeRequest(
            @Positive(message = "requestId must be greater than 0") @PathVariable Long requestId,
            @Valid @RequestBody ProcessGradeRequestRequest request
    ) {
        GradeRequestSummaryInfo info = processGradeRequestUseCase.process(
                new ProcessGradeRequestCommand(requestId, request.decision())
        );
        return ApiResponse.onSuccess(GradeRequestSummaryResponse.from(info));
    }
}
