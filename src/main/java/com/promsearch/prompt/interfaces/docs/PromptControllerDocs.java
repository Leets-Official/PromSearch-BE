package com.promsearch.prompt.interfaces.docs;

import com.promsearch.global.exception.constant.CommonErrorCode;
import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.response.PageResponse;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.interfaces.dto.request.CreatePromptRequest;
import com.promsearch.prompt.interfaces.dto.request.PromptImageUploadUrlRequest;
import com.promsearch.prompt.interfaces.dto.request.SavePromptDraftRequest;
import com.promsearch.prompt.interfaces.dto.response.MyPromptSummaryResponse;
import com.promsearch.prompt.interfaces.dto.response.PromptCommandResponse;
import com.promsearch.prompt.interfaces.dto.response.PromptDetailResponse;
import com.promsearch.prompt.interfaces.dto.response.PromptDraftResponse;
import com.promsearch.prompt.interfaces.dto.response.PromptImageUploadCompleteResponse;
import com.promsearch.prompt.interfaces.dto.response.PromptImageUploadUrlResponse;
import com.promsearch.prompt.interfaces.dto.response.PromptInsightResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 생성·임시저장·삭제·이미지 업로드 API 작업자: 한하람 (구현 중)
 */
@Tag(
        name = "Prompt | 프롬프트",
        description = "프롬프트 상세 조회·생성·단일 임시저장·삭제·이미지 업로드 API(작업자: 한하람, 상태: 구현 중), "
                + "내 게시완료 목록·인사이트 조회 API"
)
public interface PromptControllerDocs {

    @Operation(
            summary = "[PROMPT-DETAIL-001] 프롬프트 상세 조회",
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

    @Operation(
            summary = "[PROMPT-IMAGE-001] 이미지 업로드 URL 발급",
            description = "JPEG, PNG 이미지의 S3 원본 업로드용 Presigned PUT URL을 최대 10개 발급합니다. "
                    + "프론트엔드는 요청한 Content-Type을 PUT 요청에도 동일하게 전송하고, "
                    + "업로드 성공 후 완료 API를 호출해야 합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "업로드 URL 발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "파일 형식, 용량, 크기 또는 개수 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "S3 연결 또는 자격 증명 오류")
    })
    ApiResponse<PromptImageUploadUrlResponse> issueImageUploadUrls(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,

            @Valid @RequestBody PromptImageUploadUrlRequest request
    );

    @Operation(
            summary = "[PROMPT-IMAGE-002] 이미지 업로드 완료",
            description = "S3 HeadObject로 업로드 여부, Content-Type, 파일 크기를 검증하고 이미지 상태를 UPLOADED로 변경합니다. "
                    + "동일한 이미지에 대한 완료 요청은 멱등하게 처리합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "업로드 검증 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "업로드 메타데이터 불일치"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "다른 사용자의 이미지"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "이미지 자산 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "S3 객체 없음 또는 변경할 수 없는 상태"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "S3 연결 또는 자격 증명 오류")
    })
    ApiResponse<PromptImageUploadCompleteResponse> completeImageUpload(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,

            @Parameter(description = "업로드 완료를 확인할 이미지 식별자")
            @PathVariable UUID imageId
    );

    @Operation(
            summary = "[PROMPT-001] 내 임시저장 생성 또는 교체",
            description = "제목이 공백 제거 후 1자 이상일 때 저장할 수 있으며 사용자별 최신 임시저장 한 개를 생성하거나 교체합니다. "
                    + "제목 외 필드는 생략할 수 있습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "임시저장 생성 또는 교체 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "501", description = "인터페이스 계약만 작성되어 실제 저장 기능은 구현 중")
    })
    ApiResponse<PromptCommandResponse> saveDraft(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,

            @Valid @RequestBody SavePromptDraftRequest request
    );

    @Operation(
            summary = "[PROMPT-002] 내 임시저장 조회",
            description = "인증된 사용자의 최신 임시저장 한 개를 작성 중인 전체 내용과 함께 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "임시저장 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "임시저장 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "501", description = "인터페이스 계약만 작성되어 실제 조회 기능은 구현 중")
    })
    ApiResponse<PromptDraftResponse> getDraft(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user
    );

    @Operation(
            summary = "[PROMPT-003] 내 임시저장 삭제",
            description = "인증된 사용자의 최신 임시저장을 삭제합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "임시저장 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "임시저장 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "501", description = "인터페이스 계약만 작성되어 실제 삭제 기능은 구현 중")
    })
    ApiResponse<Void> deleteDraft(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user
    );

    @Operation(
            summary = "[PROMPT-004] 프롬프트 게시물 생성",
            description = "프롬프트 게시물을 생성합니다. FREE는 0포인트, PREMIUM은 추후 확정할 서버 고정 가격을 적용하며 "
                    + "요청에서 가격을 입력받지 않습니다. 원본 이미지는 백엔드 워터마크 처리 후 저장합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "프롬프트 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "501", description = "인터페이스 계약만 작성되어 실제 생성 기능은 구현 중")
    })
    ResponseEntity<ApiResponse<PromptCommandResponse>> createPrompt(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,

            @Valid @RequestBody CreatePromptRequest request
    );

    @Operation(
            summary = "[PROMPT-005] 프롬프트 게시물 삭제",
            description = "작성자 본인의 프롬프트를 즉시 일반 조회에서 제외하도록 논리 삭제합니다. "
                    + "deletedAt 기록 후 30일이 지나면 DB 데이터와 S3 이미지를 물리 삭제합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프롬프트 논리 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 프롬프트 식별자"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "작성자 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "프롬프트 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "501", description = "인터페이스 계약만 작성되어 실제 삭제 기능은 구현 중")
    })
    ApiResponse<Void> deletePrompt(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,

            @Parameter(description = "삭제할 프롬프트 식별자", example = "1")
            @Positive(message = "promptId must be greater than 0") @PathVariable Long promptId
    );

    @Operation(
            summary = "[PROMPT-006] 내 게시완료 목록 조회",
            description = "인증된 사용자가 작성한 게시완료(status=ACTIVE) 프롬프트 목록을 최신순으로 페이지네이션 조회합니다. "
                    + "논리 삭제된 게시물은 제외하며, 목록 카드에 필요한 필드만 포함하고 프롬프트 본문은 포함하지 않습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "게시완료 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "501", description = "인터페이스 계약만 작성되어 실제 조회 기능은 구현 중")
    })
    ApiResponse<PageResponse<MyPromptSummaryResponse>> getMyPublishedPrompts(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,

            @Parameter(
                    description = "조회할 게시물 처리 상태. 이 API는 게시완료된 게시물만 다루므로 ACTIVE만 지원합니다.",
                    example = "ACTIVE"
            )
            @RequestParam PromptStatus status,

            @Parameter(description = "페이지 번호(0부터 시작)", example = "0")
            @Min(value = 0, message = "page must be 0 or greater")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "페이지당 항목 수", example = "20")
            @Min(value = 1, message = "size must be 1 or greater")
            @Max(value = 100, message = "size must be 100 or less")
            @RequestParam(defaultValue = "20") int size
    );

    @Operation(
            summary = "[PROMPT-007] 내 게시글 인사이트 조회",
            description = "인증된 사용자가 작성한 전체 게시물(논리 삭제 제외) 기준으로 누적 조회수·추천수·복사수를 실시간 합산(SUM)해 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인사이트 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "501", description = "인터페이스 계약만 작성되어 실제 조회 기능은 구현 중")
    })
    ApiResponse<PromptInsightResponse> getMyPromptInsights(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user
    );
}
