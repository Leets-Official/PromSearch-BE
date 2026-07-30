package com.promsearch.prompt.interfaces.docs;

import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import com.promsearch.prompt.application.usecase.dto.HomePromptListQuery;
import com.promsearch.prompt.interfaces.dto.response.HomePromptListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Home", description = "홈 프롬프트 카드 목록 API")
public interface HomeControllerDocs {

    @Operation(
            summary = "[HOME-001] 인기 프롬프트 목록 조회",
            description = "좋아요 수 기준으로 정렬된 홈 프롬프트 카드를 조회합니다. 로그인 사용자는 liked/bookmarked 상태도 함께 받습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인기 프롬프트 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "페이지 요청 값 검증 실패")
    })
    ApiResponse<HomePromptListResponse> listPopularPrompts(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,

            @Parameter(description = "0부터 시작하는 페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") @PositiveOrZero @Max(HomePromptListQuery.MAX_PAGE) int page,

            @Parameter(description = "페이지 크기. 최대 50까지 허용합니다.", example = "12")
            @RequestParam(defaultValue = "12") @Min(1) @Max(HomePromptListQuery.MAX_SIZE) int size
    );

    @Operation(
            summary = "[HOME-002] 직군별 프롬프트 목록 조회",
            description = "JOB 타입 태그로 필터링한 홈 프롬프트 카드를 최신순으로 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "직군별 프롬프트 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "직군 태그 또는 페이지 요청 값 검증 실패")
    })
    ApiResponse<HomePromptListResponse> listJobPrompts(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,

            @Parameter(description = "JOB 타입 태그 ID", example = "1", required = true)
            @PathVariable @Positive Long jobTagId,

            @Parameter(description = "0부터 시작하는 페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") @PositiveOrZero @Max(HomePromptListQuery.MAX_PAGE) int page,

            @Parameter(description = "페이지 크기. 최대 50까지 허용합니다.", example = "12")
            @RequestParam(defaultValue = "12") @Min(1) @Max(HomePromptListQuery.MAX_SIZE) int size
    );
}
