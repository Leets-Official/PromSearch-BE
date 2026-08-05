package com.promsearch.admin.interfaces;

import com.promsearch.admin.interfaces.docs.AdminOriginUserControllerDocs;
import com.promsearch.admin.interfaces.dto.response.OriginUserSummaryResponse;
import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.response.PageResponse;
import com.promsearch.user.application.usecase.ListOriginUsersUseCase;
import com.promsearch.user.application.usecase.dto.OriginUserListInfo;
import com.promsearch.user.application.usecase.dto.OriginUserListQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/origin-users")
public class AdminOriginUserController implements AdminOriginUserControllerDocs {

    private final ListOriginUsersUseCase listOriginUsersUseCase;

    @GetMapping
    @Override
    public ApiResponse<PageResponse<OriginUserSummaryResponse>> getOriginUsers(
            @Min(value = 0, message = "page must be 0 or greater")
            @RequestParam(defaultValue = "0") int page,
            @Min(value = 1, message = "size must be 1 or greater")
            @Max(value = 100, message = "size must be 100 or less")
            @RequestParam(defaultValue = "20") int size
    ) {
        OriginUserListInfo info = listOriginUsersUseCase.list(new OriginUserListQuery(page, size));
        PageResponse<OriginUserSummaryResponse> response = PageResponse.of(
                info.content().stream().map(OriginUserSummaryResponse::from).toList(),
                info.page(),
                info.size(),
                info.totalElements()
        );
        return ApiResponse.onSuccess(response);
    }
}
