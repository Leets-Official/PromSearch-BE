package com.promsearch.prompt.interfaces.docs;

import com.promsearch.global.exception.constant.CommonErrorCode;
import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import com.promsearch.prompt.interfaces.dto.PromptDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Prompt | 프롬프트", description = "프롬프트 상세 조회 API")
public interface PromptControllerDocs {

    @Operation(
            summary = "[PROMPT-001] 프롬프트 상세 조회",
            description = """
                    프롬프트 상세 정보를 조회합니다. 인증 토큰은 선택 사항입니다.
                    비회원에게는 promptBody를 빈 문자열로 반환합니다.
                    PREMIUM 미결제 회원에게는 원문 앞부분 10% 이내이면서 최대 200자만 반환합니다.
                    로그인 사용자의 좋아요 및 북마크 여부를 viewerInteraction으로 반환합니다.
                    비로그인 사용자의 viewerInteraction 값은 모두 false입니다.
                    이미지 URL은 워터마크 결과물의 Presigned URL만 제공합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "유효하지 않은 프롬프트 ID",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = CommonErrorCode.Examples.BAD_REQUEST)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "프롬프트 없음 또는 조회 불가",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = CommonErrorCode.Examples.NOT_FOUND)
                    )
            )
    })
    ApiResponse<PromptDetailResponse> getPromptDetail(
            @Parameter(description = "프롬프트 ID", example = "10", required = true)
            @Positive(message = "promptId must be positive")
            @PathVariable Long promptId,

            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user
    );
}
