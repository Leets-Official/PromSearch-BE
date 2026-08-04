package com.promsearch.prompt.interfaces;

import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import com.promsearch.prompt.application.usecase.ListHomePromptsUseCase;
import com.promsearch.prompt.application.usecase.dto.HomePromptListInfo;
import com.promsearch.prompt.application.usecase.dto.HomePromptListQuery;
import com.promsearch.prompt.application.usecase.dto.HomePromptSort;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import com.promsearch.prompt.interfaces.docs.HomeControllerDocs;
import com.promsearch.prompt.interfaces.dto.response.HomePromptListResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/home/prompts")
public class HomeController implements HomeControllerDocs {

    /*
     * RequestParam의 defaultValue는 문자열 상수만 사용할 수 있으므로 문자열로 선언합니다.
     * 홈 화면은 첫 진입에서 12개 카드 단위로 렌더링하는 흐름을 기준으로 기본 size를 12로 둡니다.
     */
    private static final String DEFAULT_PAGE = "0";
    private static final String DEFAULT_SIZE = "12";

    private final ListHomePromptsUseCase listHomePromptsUseCase;

    @GetMapping
    @SecurityRequirements
    @Override
    public ApiResponse<HomePromptListResponse> listPrompts(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @RequestParam(required = false) @Positive Long jobTagId,
            @RequestParam(required = false) @Size(max = HomePromptListQuery.MAX_FILTER_TAGS) List<@Positive Long> taskTagIds,
            @RequestParam(required = false) @Size(max = HomePromptListQuery.MAX_FILTER_TAGS) List<@Positive Long> aiModelTagIds,
            @RequestParam(required = false) List<PromptOutputType> outputTypes,
            @RequestParam(name = "q", required = false) @Size(max = HomePromptListQuery.MAX_KEYWORD_LENGTH) String q,
            @RequestParam(defaultValue = "LATEST") HomePromptSort sort,
            @RequestParam(defaultValue = DEFAULT_PAGE) @PositiveOrZero @Max(HomePromptListQuery.MAX_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) @Min(1) @Max(HomePromptListQuery.MAX_SIZE) int size
    ) {
        /*
         * 화면 필터 값은 그대로 서비스에 흘려보내지 않고 HomePromptListQuery로 한 번 묶습니다.
         * 이렇게 하면 중복 태그 제거, 검색어 공백 정리, 페이지 범위 방어가 application 경계에서 한 번 더 적용됩니다.
         */
        HomePromptListInfo result = listHomePromptsUseCase.listPrompts(HomePromptListQuery.filtered(
                viewerUserId(user),
                jobTagId,
                taskTagIds,
                aiModelTagIds,
                outputTypes,
                q,
                sort,
                page,
                size
        ));
        return ApiResponse.onSuccess(HomePromptListResponse.from(result));
    }

    @GetMapping("/popular")
    @SecurityRequirements
    @Override
    public ApiResponse<HomePromptListResponse> listPopularPrompts(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @RequestParam(defaultValue = DEFAULT_PAGE) @PositiveOrZero @Max(HomePromptListQuery.MAX_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) @Min(1) @Max(HomePromptListQuery.MAX_SIZE) int size
    ) {
        /*
         * 인기 목록도 홈 탐색 영역이라 비회원 조회를 허용합니다.
         * 로그인 사용자인 경우에만 userId를 넘겨 카드별 liked/bookmarked 상태를 함께 계산합니다.
         */
        HomePromptListInfo result = listHomePromptsUseCase.listPopularPrompts(
                HomePromptListQuery.popular(viewerUserId(user), page, size)
        );
        return ApiResponse.onSuccess(HomePromptListResponse.from(result));
    }

    @GetMapping("/jobs/{jobTagId}")
    @SecurityRequirements
    @Override
    public ApiResponse<HomePromptListResponse> listJobPrompts(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @PathVariable @Positive Long jobTagId,
            @RequestParam(defaultValue = DEFAULT_PAGE) @PositiveOrZero @Max(HomePromptListQuery.MAX_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) @Min(1) @Max(HomePromptListQuery.MAX_SIZE) int size
    ) {
        /*
         * jobTagId는 화면 표시명("학생", "개발자")이 아니라 tags 테이블의 JOB 타입 태그 ID입니다.
         * persistence query에서도 tagType = JOB 조건을 함께 걸어 다른 타입 태그 ID가 섞여 들어오는 일을 막습니다.
         */
        HomePromptListInfo result = listHomePromptsUseCase.listJobPrompts(
                HomePromptListQuery.job(viewerUserId(user), jobTagId, page, size)
        );
        return ApiResponse.onSuccess(HomePromptListResponse.from(result));
    }

    private Long viewerUserId(AuthenticatedUserPrincipal user) {
        /*
         * @AuthenticationPrincipal은 비회원 공개 API에서 null일 수 있습니다.
         * null을 명시적으로 허용해 게스트 조회와 로그인 조회가 같은 query 모델을 사용하게 합니다.
         */
        return user == null ? null : user.userId();
    }
}
