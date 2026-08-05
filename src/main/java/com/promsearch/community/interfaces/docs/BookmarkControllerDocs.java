package com.promsearch.community.interfaces.docs;

import com.promsearch.community.application.usecase.dto.BookmarkListQuery;
import com.promsearch.community.interfaces.dto.response.BookmarkListResponse;
import com.promsearch.community.interfaces.dto.response.BookmarkResponse;
import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(
        name = "Community | 커뮤니티",
        description = "프롬프트 좋아요·북마크·댓글 등 사용자 상호작용 API"
)
public interface BookmarkControllerDocs {

    String IMPLEMENTED_BY_HANHARAM = "**작업자: 한하람 | 구현 상태: 구현완료**\n\n";

    @Operation(
            summary = "[COMMUNITY-003] 프롬프트 북마크 등록",
            description = IMPLEMENTED_BY_HANHARAM
                    + "JWT 인증 사용자가 공개된 활성 프롬프트를 북마크합니다. "
                    + "같은 프롬프트를 중복 북마크하면 409를 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "북마크 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 프롬프트 ID"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "북마크할 수 있는 프롬프트 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 북마크한 프롬프트")
    })
    ResponseEntity<ApiResponse<BookmarkResponse>> bookmark(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,

            @Positive(message = "promptId must be greater than 0")
            @PathVariable Long promptId
    );

    @Operation(
            summary = "[COMMUNITY-004] 프롬프트 북마크 취소",
            description = IMPLEMENTED_BY_HANHARAM
                    + "JWT 인증 사용자의 북마크를 취소합니다. "
                    + "북마크 기록이 없어도 bookmarked=false로 성공하는 멱등 API입니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "북마크 취소 또는 이미 취소됨"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 프롬프트 ID"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    ApiResponse<BookmarkResponse> unbookmark(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,

            @Positive(message = "promptId must be greater than 0")
            @PathVariable Long promptId
    );

    @Operation(
            summary = "[COMMUNITY-005] 내 북마크 목록 조회",
            description = IMPLEMENTED_BY_HANHARAM
                    + "JWT 인증 사용자가 북마크한 공개 활성 프롬프트를 최신 북마크순으로 조회합니다. "
                    + "태스크·AI 모델 태그와 결과물 타입 필터를 조합할 수 있습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "북마크 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 필터 또는 페이지 정보"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    ApiResponse<BookmarkListResponse> list(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,

            @Parameter(description = "TASK 태그 식별자 목록 (콤마 구분)")
            @Size(max = BookmarkListQuery.MAX_FILTER_TAGS, message = "taskTagIds size must be " + BookmarkListQuery.MAX_FILTER_TAGS + " or less")
            @RequestParam(required = false) List<@Positive(message = "taskTagIds must contain values greater than 0") Long> taskTagIds,

            @Parameter(description = "AI_MODEL 태그 식별자 목록 (콤마 구분)")
            @Size(max = BookmarkListQuery.MAX_FILTER_TAGS, message = "aiModelTagIds size must be " + BookmarkListQuery.MAX_FILTER_TAGS + " or less")
            @RequestParam(required = false) List<@Positive(message = "aiModelTagIds must contain values greater than 0") Long> aiModelTagIds,

            @Parameter(description = "결과물 타입 목록 (콤마 구분)")
            @RequestParam(required = false) List<PromptOutputType> outputTypes,

            @Min(value = 0, message = "page must be greater than or equal to 0")
            @RequestParam(defaultValue = "0") int page,

            @Min(value = 1, message = "size must be greater than 0")
            @Max(value = 50, message = "size must be less than or equal to 50")
            @RequestParam(defaultValue = "12") int size
    );
}
