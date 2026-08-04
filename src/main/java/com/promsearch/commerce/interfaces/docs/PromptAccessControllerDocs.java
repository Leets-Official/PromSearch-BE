package com.promsearch.commerce.interfaces.docs;

import com.promsearch.commerce.interfaces.dto.response.PromptCopyResponse;
import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Commerce | 프롬프트 접근", description = "유료 프롬프트 잠금 해제 및 본문 복사 API")
@SecurityRequirement(name = "jwtBearerAuth")
public interface PromptAccessControllerDocs {

    String IMPLEMENTED_BY_KUNHEELEE7 = "**작업자: KunHeeLee7 | 구현 상태: 구현완료**\n\n";

    @Operation(
            summary = "[COMMERCE-001] 프롬프트 잠금 해제",
            description = IMPLEMENTED_BY_KUNHEELEE7
                    + "PREMIUM 프롬프트의 본문 접근 권한을 멱등하게 저장합니다. "
                    + "FREE 프롬프트와 작성자 본인의 프롬프트는 별도 레코드 없이 접근 가능한 상태를 반환합니다. "
                    + "성공 후 상세 조회 API를 재호출하면 전문을 확인할 수 있습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "잠금 해제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 프롬프트 ID"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "접근 가능한 프롬프트 없음")
    })
    ApiResponse<Void> unlock(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,

            @Parameter(description = "잠금 해제할 프롬프트 ID", example = "10")
            @Positive(message = "promptId must be greater than 0")
            @PathVariable Long promptId
    );

    @Operation(
            summary = "[COMMERCE-002] 프롬프트 본문 복사",
            description = IMPLEMENTED_BY_KUNHEELEE7
                    + "FREE 프롬프트, 작성자 본인의 프롬프트 또는 잠금 해제한 PREMIUM 프롬프트의 전문을 반환합니다. "
                    + "작성자를 제외한 사용자별 최초 복사만 copyCount에 반영합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프롬프트 전문 반환"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 프롬프트 ID"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "유료 프롬프트 복사 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "접근 가능한 프롬프트 없음")
    })
    ApiResponse<PromptCopyResponse> copy(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,

            @Parameter(description = "복사할 프롬프트 ID", example = "10")
            @Positive(message = "promptId must be greater than 0")
            @PathVariable Long promptId
    );
}
