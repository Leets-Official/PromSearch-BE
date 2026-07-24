package com.promsearch.prompt.interfaces.docs;

import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import com.promsearch.prompt.interfaces.dto.CreatePromptRequest;
import com.promsearch.prompt.interfaces.dto.PromptCommandResponse;
import com.promsearch.prompt.interfaces.dto.PromptDraftResponse;
import com.promsearch.prompt.interfaces.dto.PromptImageUploadUrlRequest;
import com.promsearch.prompt.interfaces.dto.PromptImageUploadUrlResponse;
import com.promsearch.prompt.interfaces.dto.SavePromptDraftRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 작업자: 한하람
 * 구현 상태: 구현 중
 */
@Tag(
        name = "Prompt | 프롬프트",
        description = "프롬프트 생성·단일 임시저장·삭제·이미지 업로드 API | 작업자: 한하람 | 상태: 구현 중"
)
public interface PromptControllerDocs {

    @Operation(
            summary = "[PROMPT-IMAGE-001] 이미지 업로드 URL 발급",
            description = "JPEG, PNG, WebP 이미지의 S3 임시 업로드용 Presigned URL을 최대 10개 발급합니다. "
                    + "프론트엔드는 업로드 완료 후 응답의 imageId만 프롬프트 저장 요청에 전달합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "업로드 URL 발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "파일 형식, 용량, 크기 또는 개수 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "501", description = "인터페이스 계약만 작성되어 실제 발급 기능은 구현 중")
    })
    ApiResponse<PromptImageUploadUrlResponse> issueImageUploadUrls(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,

            @Valid @RequestBody PromptImageUploadUrlRequest request
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
}
