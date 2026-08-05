package com.promsearch.community.interfaces.docs;

import com.promsearch.community.interfaces.dto.response.LikeResponse;
import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(
        name = "Community | 커뮤니티",
        description = "프롬프트 좋아요·북마크·댓글 등 사용자 상호작용 API"
)
public interface LikeControllerDocs {

    String IMPLEMENTED_BY_HANHARAM = "**작업자: 한하람 | 구현 상태: 구현완료**\n\n";

    @Operation(
            summary = "[COMMUNITY-001] 프롬프트 좋아요 등록",
            description = IMPLEMENTED_BY_HANHARAM
                    + "JWT 인증 사용자가 공개된 활성 프롬프트에 좋아요를 등록합니다. "
                    + "이미 좋아요한 프롬프트에 다시 요청하면 409를 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "좋아요 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 프롬프트 ID"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "좋아요할 수 있는 프롬프트 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 좋아요한 프롬프트")
    })
    ResponseEntity<ApiResponse<LikeResponse>> like(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,

            @Parameter(description = "좋아요할 프롬프트 식별자", example = "10")
            @Positive(message = "promptId must be greater than 0")
            @PathVariable Long promptId
    );

    @Operation(
            summary = "[COMMUNITY-002] 프롬프트 좋아요 취소",
            description = IMPLEMENTED_BY_HANHARAM
                    + "JWT 인증 사용자가 본인이 등록한 좋아요를 취소합니다. "
                    + "등록된 좋아요가 없으면 404를 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "좋아요 취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 프롬프트 ID"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "등록된 좋아요 또는 프롬프트 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "좋아요 수 정합성 오류")
    })
    ApiResponse<LikeResponse> unlike(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,

            @Parameter(description = "좋아요를 취소할 프롬프트 식별자", example = "10")
            @Positive(message = "promptId must be greater than 0")
            @PathVariable Long promptId
    );
}
