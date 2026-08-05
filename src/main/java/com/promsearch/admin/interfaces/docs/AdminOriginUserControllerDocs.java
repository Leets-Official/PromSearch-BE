package com.promsearch.admin.interfaces.docs;

import com.promsearch.admin.interfaces.dto.response.OriginUserSummaryResponse;
import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(
        name = "Admin | Origin 등급업",
        description = "Origin 심사 대기 목록 조회 및 승인/반려 API. 유저가 자동 승급으로 Prime에 도달하면 "
                + "심사 대기 항목이 자동 생성되며, 관리자는 이 목록을 조회하고 승인/반려만 처리합니다."
)
public interface AdminOriginUserControllerDocs {

    String IMPLEMENTED_BY_KALLIN1 = "**작업자: kallin1 | 구현 상태: 구현완료**\n\n";

    @Operation(
            summary = "[ADMIN-GRADE-003] Origin 등급 유저 목록 조회",
            description = IMPLEMENTED_BY_KALLIN1 + "현재 Origin 등급인 유저 목록을 페이지네이션 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Origin 유저 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요")
    })
    ApiResponse<PageResponse<OriginUserSummaryResponse>> getOriginUsers(
            @Parameter(description = "페이지 번호(0부터 시작)", example = "0")
            @Min(value = 0, message = "page must be 0 or greater")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "페이지당 항목 수", example = "20")
            @Min(value = 1, message = "size must be 1 or greater")
            @Max(value = 100, message = "size must be 100 or less")
            @RequestParam(defaultValue = "20") int size
    );
}
