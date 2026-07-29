package com.promsearch.community.interfaces.docs;

import com.promsearch.community.interfaces.dto.request.CreateCommentRequest;
import com.promsearch.community.interfaces.dto.request.UpdateCommentRequest;
import com.promsearch.community.interfaces.dto.response.CommentListResponse;
import com.promsearch.community.interfaces.dto.response.CommentReplyResponse;
import com.promsearch.community.interfaces.dto.response.CommentResponse;
import com.promsearch.global.exception.constant.CommonErrorCode;
import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Comment | 댓글", description = "댓글 조회·작성·수정·삭제 및 대댓글 작성 API")
public interface CommentControllerDocs {

    @Operation(
            summary = "[COMMENT-001] 댓글 목록 조회",
            description = """
                    프롬프트의 댓글과 대댓글 전체를 작성 시간 내림차순으로 조회합니다. 인증 토큰은 선택 사항입니다.
                    mine은 로그인 사용자 본인의 댓글 여부이고, promptAuthor는 프롬프트 작성자의 댓글 여부입니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "댓글 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "부모 댓글과 대댓글",
                                    summary = "부모 댓글의 replies에 대댓글이 포함된 경우",
                                    value = """
                                            {
                                              "success": true,
                                              "code": "COMMON-200",
                                              "message": "성공했습니다.",
                                              "result": {
                                                "comments": [
                                                  {
                                                    "commentId": 103,
                                                    "parentCommentId": null,
                                                    "author": {
                                                      "userId": 10,
                                                      "nickname": "이영희",
                                                      "profileImageUrl": null
                                                    },
                                                    "content": "저도 잘 사용했습니다.",
                                                    "status": "ACTIVE",
                                                    "mine": false,
                                                    "promptAuthor": true,
                                                    "createdAt": "2026-07-23T04:00:00Z",
                                                    "updatedAt": "2026-07-23T04:00:00Z",
                                                    "replies": []
                                                  },
                                                  {
                                                    "commentId": 101,
                                                    "parentCommentId": null,
                                                    "author": {
                                                      "userId": 8,
                                                      "nickname": "홍길동",
                                                      "profileImageUrl": "https://cdn.promsearch.com/profiles/8.jpg"
                                                    },
                                                    "content": "좋은 프롬프트네요.",
                                                    "status": "ACTIVE",
                                                    "mine": false,
                                                    "promptAuthor": false,
                                                    "createdAt": "2026-07-23T03:00:00Z",
                                                    "updatedAt": "2026-07-23T03:00:00Z",
                                                    "replies": [
                                                      {
                                                        "commentId": 102,
                                                        "parentCommentId": 101,
                                                        "author": {
                                                          "userId": 9,
                                                          "nickname": "김철수",
                                                          "profileImageUrl": null
                                                        },
                                                        "content": "저도 그렇게 생각합니다.",
                                                        "status": "ACTIVE",
                                                        "mine": true,
                                                        "promptAuthor": false,
                                                        "createdAt": "2026-07-23T03:10:00Z",
                                                        "updatedAt": "2026-07-23T03:10:00Z"
                                                      }
                                                    ]
                                                  }
                                                ]
                                              }
                                            }
                                            """
                            )
                    )
            ),
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
                    description = "프롬프트 없음",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = CommonErrorCode.Examples.NOT_FOUND)
                    )
            )
    })
    ApiResponse<CommentListResponse> getComments(
            @Parameter(description = "프롬프트 ID", example = "10", required = true)
            @PathVariable Long promptId,

            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user
    );

    @Operation(summary = "[COMMENT-002] 댓글 작성", description = "인증된 사용자가 프롬프트에 최상위 댓글을 작성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "댓글 작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 값 검증 실패",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = CommonErrorCode.Examples.BAD_REQUEST)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = CommonErrorCode.Examples.UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "프롬프트 없음",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = CommonErrorCode.Examples.NOT_FOUND)
                    )
            )
    })
    ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @Parameter(description = "프롬프트 ID", example = "10", required = true)
            @Positive(message = "promptId must be positive")
            @PathVariable Long promptId,

            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,

            @Valid @RequestBody CreateCommentRequest request
    );

    @Operation(summary = "[COMMENT-003] 댓글 수정", description = "댓글 작성자 본인이 댓글 내용을 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "댓글 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 값 검증 실패",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = CommonErrorCode.Examples.BAD_REQUEST)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = CommonErrorCode.Examples.UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "수정 권한 없음",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = CommonErrorCode.Examples.FORBIDDEN)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "댓글 없음",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = CommonErrorCode.Examples.NOT_FOUND)
                    )
            )
    })
    ApiResponse<CommentResponse> updateComment(
            @Parameter(description = "댓글 ID", example = "101", required = true)
            @Positive(message = "commentId must be positive")
            @PathVariable Long commentId,

            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,

            @Valid @RequestBody UpdateCommentRequest request
    );

    @Operation(summary = "[COMMENT-004] 댓글 삭제", description = "댓글 작성자 본인이 댓글을 논리 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "댓글 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = CommonErrorCode.Examples.UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "삭제 권한 없음",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = CommonErrorCode.Examples.FORBIDDEN)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "댓글 없음",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = CommonErrorCode.Examples.NOT_FOUND)
                    )
            )
    })
    ApiResponse<Void> deleteComment(
            @Parameter(description = "댓글 ID", example = "101", required = true)
            @Positive(message = "commentId must be positive")
            @PathVariable Long commentId,

            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user
    );

    @Operation(
            summary = "[COMMENT-005] 대댓글 작성",
            description = "인증된 사용자가 최상위 댓글에 대댓글을 작성합니다. 대댓글에는 추가 대댓글을 작성할 수 없습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "대댓글 작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 값 검증 실패 또는 대댓글에 답글 작성 시도",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = CommonErrorCode.Examples.BAD_REQUEST)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = CommonErrorCode.Examples.UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "부모 댓글 없음",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = CommonErrorCode.Examples.NOT_FOUND)
                    )
            )
    })
    ResponseEntity<ApiResponse<CommentReplyResponse>> createReply(
            @Parameter(description = "부모 댓글 ID", example = "101", required = true)
            @Positive(message = "commentId must be positive")
            @PathVariable Long commentId,

            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,

            @Valid @RequestBody CreateCommentRequest request
    );
}
