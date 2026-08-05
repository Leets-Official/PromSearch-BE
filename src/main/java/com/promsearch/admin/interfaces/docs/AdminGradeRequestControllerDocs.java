package com.promsearch.admin.interfaces.docs;

import com.promsearch.admin.interfaces.dto.request.ProcessGradeRequestRequest;
import com.promsearch.admin.interfaces.dto.response.GradeRequestSummaryResponse;
import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.response.PageResponse;
import com.promsearch.user.domain.enums.GradeRequestStatus;
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

@Tag(
        name = "Admin | Origin 등급업",
        description = "Origin 심사 대기 목록 조회 및 승인/반려 API. 유저가 자동 승급으로 Prime에 도달하면 "
                + "심사 대기 항목이 자동 생성되며, 관리자는 이 목록을 조회하고 승인/반려만 처리합니다."
)
public interface AdminGradeRequestControllerDocs {

    String IMPLEMENTED_BY_KALLIN1 = "**작업자: kallin1 | 구현 상태: 구현완료**\n\n";

    @Operation(
            summary = "[ADMIN-GRADE-001] Origin 심사 대기 목록 조회",
            description = IMPLEMENTED_BY_KALLIN1
                    + "처리 상태로 필터링해 Origin 심사 대기 목록을 페이지네이션 조회합니다. "
                    + "각 항목은 신청자 정보와 함께 게시글 수, 누적 추천 수를 포함합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "심사 대기 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요")
    })
    ApiResponse<PageResponse<GradeRequestSummaryResponse>> getGradeRequests(
            @Parameter(description = "처리 상태 필터", example = "PENDING")
            @RequestParam(required = false) GradeRequestStatus status,

            @Parameter(description = "페이지 번호(0부터 시작)", example = "0")
            @Min(value = 0, message = "page must be 0 or greater")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "페이지당 항목 수", example = "20")
            @Min(value = 1, message = "size must be 1 or greater")
            @Max(value = 100, message = "size must be 100 or less")
            @RequestParam(defaultValue = "20") int size
    );

    @Operation(
            summary = "[ADMIN-GRADE-002] Origin 심사 대기 항목 승인/반려",
            description = IMPLEMENTED_BY_KALLIN1
                    + "심사 대기 항목을 승인하거나 반려합니다. 승인 시 항목 상태 변경과 유저 등급 변경(Origin)을 "
                    + "하나의 트랜잭션으로 처리하며, 반려 시 유저 등급은 변경하지 않습니다. "
                    + "이미 처리된 항목을 다시 처리하려 하면 409를 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "심사 대기 항목 처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "심사 대기 항목 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 처리된 항목")
    })
    ApiResponse<GradeRequestSummaryResponse> processGradeRequest(
            @Parameter(description = "처리할 등급업 신청 식별자", example = "1")
            @Positive(message = "requestId must be greater than 0") @PathVariable Long requestId,

            @Valid @RequestBody ProcessGradeRequestRequest request
    );
}
