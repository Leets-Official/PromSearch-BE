package com.promsearch.prompt.interfaces;

import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import com.promsearch.prompt.application.usecase.ListHomePromptsUseCase;
import com.promsearch.prompt.application.usecase.dto.HomePromptListInfo;
import com.promsearch.prompt.application.usecase.dto.HomePromptListQuery;
import com.promsearch.prompt.interfaces.docs.HomeControllerDocs;
import com.promsearch.prompt.interfaces.dto.response.HomePromptListResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
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

    @GetMapping("/popular")
    @SecurityRequirements
    @Override
    public ApiResponse<HomePromptListResponse> listPopularPrompts(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @RequestParam(defaultValue = DEFAULT_PAGE) @PositiveOrZero @Max(HomePromptListQuery.MAX_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) @Min(1) @Max(HomePromptListQuery.MAX_SIZE) int size
    ) {
        /*
         * 홈 목록은 비회원도 조회할 수 있습니다.
         * 다만 로그인 사용자인 경우 카드마다 liked/bookmarked 상태를 내려줘야 하므로
         * 인증 객체가 있으면 userId를 query에 포함하고, 없으면 null로 전달합니다.
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
         * jobTagId는 단순 문자열이 아니라 tags 테이블의 JOB 타입 태그 ID입니다.
         * 실제 타입 검증은 persistence query에서 tagType = JOB 조건으로 한 번 더 제한합니다.
         */
        HomePromptListInfo result = listHomePromptsUseCase.listJobPrompts(
                HomePromptListQuery.job(viewerUserId(user), jobTagId, page, size)
        );
        return ApiResponse.onSuccess(HomePromptListResponse.from(result));
    }

    private Long viewerUserId(AuthenticatedUserPrincipal user) {
        /*
         * @AuthenticationPrincipal은 비회원 공개 API에서 null일 수 있습니다.
         * null을 명시적으로 허용해 application 계층이 게스트 조회와 로그인 조회를 같은 query 모델로 처리하게 합니다.
         */
        return user == null ? null : user.userId();
    }
}
