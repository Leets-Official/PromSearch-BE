package com.promsearch.prompt.interfaces.docs;

import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import com.promsearch.prompt.interfaces.dto.HomePromptListResponse;
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

@Tag(name = "Home", description = "Home prompt card list APIs")
public interface HomeControllerDocs {

    @Operation(
            summary = "[HOME-001] List popular prompts",
            description = "Returns public home cards sorted by like count. Logged-in viewers also receive liked/bookmarked flags."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Popular prompt list found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid paging request")
    })
    ApiResponse<HomePromptListResponse> listPopularPrompts(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,

            @Parameter(description = "Zero-based page number", example = "0")
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,

            @Parameter(description = "Page size. Max value is 50.", example = "12")
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size
    );

    @Operation(
            summary = "[HOME-002] List prompts by job tag",
            description = "Returns public home cards filtered by a JOB tag and sorted by newest first."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Job prompt list found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid job tag or paging request")
    })
    ApiResponse<HomePromptListResponse> listJobPrompts(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,

            @Parameter(description = "JOB tag id", example = "1", required = true)
            @PathVariable @Positive Long jobTagId,

            @Parameter(description = "Zero-based page number", example = "0")
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,

            @Parameter(description = "Page size. Max value is 50.", example = "12")
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size
    );
}
