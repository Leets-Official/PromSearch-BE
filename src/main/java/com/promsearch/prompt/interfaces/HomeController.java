package com.promsearch.prompt.interfaces;

import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import com.promsearch.prompt.application.HomePromptListInfo;
import com.promsearch.prompt.application.HomePromptListQuery;
import com.promsearch.prompt.application.ListHomePromptsUseCase;
import com.promsearch.prompt.interfaces.docs.HomeControllerDocs;
import com.promsearch.prompt.interfaces.dto.HomePromptListResponse;
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

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 12;

    private final ListHomePromptsUseCase listHomePromptsUseCase;

    @GetMapping("/popular")
    @Override
    public ApiResponse<HomePromptListResponse> listPopularPrompts(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) @PositiveOrZero int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) @Min(1) @Max(50) int size
    ) {
        HomePromptListInfo result = listHomePromptsUseCase.listPopularPrompts(
                HomePromptListQuery.popular(viewerUserId(user), page, size)
        );
        return ApiResponse.onSuccess(HomePromptListResponse.from(result));
    }

    @GetMapping("/jobs/{jobTagId}")
    @Override
    public ApiResponse<HomePromptListResponse> listJobPrompts(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @PathVariable @Positive Long jobTagId,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) @PositiveOrZero int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) @Min(1) @Max(50) int size
    ) {
        HomePromptListInfo result = listHomePromptsUseCase.listJobPrompts(
                HomePromptListQuery.job(viewerUserId(user), jobTagId, page, size)
        );
        return ApiResponse.onSuccess(HomePromptListResponse.from(result));
    }

    private Long viewerUserId(AuthenticatedUserPrincipal user) {
        return user == null ? null : user.userId();
    }
}
