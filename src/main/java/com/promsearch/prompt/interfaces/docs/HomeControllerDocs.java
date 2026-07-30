package com.promsearch.prompt.interfaces.docs;

import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import com.promsearch.prompt.application.usecase.dto.HomePromptListQuery;
import com.promsearch.prompt.application.usecase.dto.HomePromptSort;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import com.promsearch.prompt.interfaces.dto.response.HomePromptListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Home", description = "홈 프롬프트 카드 목록 API")
public interface HomeControllerDocs {

    String IMPLEMENTED_BY_RUCHAN04 = "**작업자: ruchan04 | 구현 상태: 구현완료**\n\n";

    @Operation(
            summary = "[HOME-001] 홈 프롬프트 필터 목록 조회",
            description = IMPLEMENTED_BY_RUCHAN04
                    + """
                    홈 화면의 검색창, 좌측 직군 메뉴, 태스크 다중 선택, AI 모델, 결과물 타입 필터를 조합해 카드 목록을 조회합니다.
                    비회원도 조회할 수 있고, 로그인 사용자는 liked/bookmarked 상태를 함께 받습니다.
                    태그 조건은 새 태그를 만들지 않고 tags 테이블에 이미 존재하는 태그 ID만 조회 조건으로 사용합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "홈 프롬프트 필터 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "필터 또는 페이지 요청 값 검증 실패")
    })
    ApiResponse<HomePromptListResponse> listPrompts(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,

            @Parameter(description = "JOB 타입 태그 ID. 전체 직군이면 생략합니다.", example = "1")
            @RequestParam(required = false) @Positive Long jobTagId,

            @Parameter(description = "TASK 타입 태그 ID 목록. 여러 개를 선택하면 OR 조건으로 조회합니다.", example = "10,11,12")
            @RequestParam(required = false) @Size(max = HomePromptListQuery.MAX_FILTER_TAGS) List<@Positive Long> taskTagIds,

            @Parameter(description = "AI_MODEL 타입 태그 ID. 전체 모델이면 생략합니다.", example = "20")
            @RequestParam(required = false) @Positive Long aiModelTagId,

            @Parameter(description = "결과물 타입. 전체 결과물이면 생략합니다.", example = "TEXT")
            @RequestParam(required = false) PromptOutputType outputType,

            @Parameter(description = "제목, 설명, 태그명에 적용할 검색어", example = "보고서")
            @RequestParam(required = false) @Size(max = HomePromptListQuery.MAX_KEYWORD_LENGTH) String keyword,

            @Parameter(description = "정렬 기준. LATEST는 최신순, POPULAR는 좋아요순입니다.", example = "LATEST")
            @RequestParam(defaultValue = "LATEST") HomePromptSort sort,

            @Parameter(description = "0부터 시작하는 페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") @PositiveOrZero @Max(HomePromptListQuery.MAX_PAGE) int page,

            @Parameter(description = "페이지 크기. 최대 50까지 허용합니다.", example = "12")
            @RequestParam(defaultValue = "12") @Min(1) @Max(HomePromptListQuery.MAX_SIZE) int size
    );

    @Operation(
            summary = "[HOME-002] 인기 프롬프트 목록 조회",
            description = IMPLEMENTED_BY_RUCHAN04
                    + "좋아요 수 기준으로 정렬된 홈 프롬프트 카드를 조회합니다. 로그인 사용자는 liked/bookmarked 상태도 함께 받습니다."
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
            summary = "[HOME-003] 직군별 프롬프트 목록 조회",
            description = IMPLEMENTED_BY_RUCHAN04
                    + "JOB 타입 태그로 필터링한 홈 프롬프트 카드를 최신순으로 조회합니다."
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
