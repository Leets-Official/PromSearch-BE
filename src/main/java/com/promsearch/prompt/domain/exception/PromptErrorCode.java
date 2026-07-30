package com.promsearch.prompt.domain.exception;

import com.promsearch.global.response.code.BaseCode;
import org.springframework.http.HttpStatus;

public enum PromptErrorCode implements BaseCode {

    PROMPT_NOT_FOUND(HttpStatus.NOT_FOUND, "PROMPT-001", "프롬프트를 찾을 수 없습니다."),
    INVALID_PROMPT_TITLE(HttpStatus.BAD_REQUEST, "PROMPT-002", "프롬프트 제목이 유효하지 않습니다."),
    INVALID_PROMPT_BODY(HttpStatus.BAD_REQUEST, "PROMPT-003", "프롬프트 본문이 유효하지 않습니다."),
    INVALID_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "PROMPT-004", "콘텐츠 타입이 유효하지 않습니다."),
    INVALID_OUTPUT_TYPE(HttpStatus.BAD_REQUEST, "PROMPT-005", "결과물 타입이 유효하지 않습니다."),
    INVALID_PRICE_POINT(HttpStatus.BAD_REQUEST, "PROMPT-006", "프롬프트 가격이 유효하지 않습니다."),
    PROMPT_NOT_OWNED(HttpStatus.FORBIDDEN, "PROMPT-007", "본인의 프롬프트만 수정/삭제할 수 있습니다."),
    PAID_PROMPT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "PROMPT-008", "유료 프롬프트 열람 권한이 없습니다."),
    PROMPT_ALREADY_DELETED(HttpStatus.CONFLICT, "PROMPT-009", "이미 삭제된 프롬프트입니다."),
    TAG_NOT_FOUND(HttpStatus.NOT_FOUND, "PROMPT-010", "태그를 찾을 수 없습니다."),
    INVALID_TAG_TYPE(HttpStatus.BAD_REQUEST, "PROMPT-011", "태그 종류가 유효하지 않습니다."),
    INVALID_TAG_NAME(HttpStatus.BAD_REQUEST, "PROMPT-012", "태그명이 유효하지 않습니다."),
    DUPLICATE_TAG(HttpStatus.CONFLICT, "PROMPT-013", "이미 존재하는 태그입니다."),
    CUSTOM_TAG_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "PROMPT-014", "해당 태그 종류는 사용자 생성이 허용되지 않습니다."),
    POST_TAG_NOT_FOUND(HttpStatus.NOT_FOUND, "PROMPT-015", "포스트 태그 정보를 찾을 수 없습니다."),
    INVALID_ID(HttpStatus.BAD_REQUEST, "PROMPT-016", "식별자가 유효하지 않습니다."),
    INVALID_PROMPT_USER_ID(HttpStatus.BAD_REQUEST, "PROMPT-017", "프롬프트 작성자 식별자가 유효하지 않습니다."),
    INVALID_PROMPT_STATUS(HttpStatus.BAD_REQUEST, "PROMPT-018", "프롬프트 상태가 유효하지 않습니다."),
    INVALID_IMAGE_URL(HttpStatus.BAD_REQUEST, "PROMPT-019", "이미지 주소가 유효하지 않습니다."),
    INVALID_IMAGE_ORDER(HttpStatus.BAD_REQUEST, "PROMPT-020", "이미지 순서가 유효하지 않습니다."),
    INVALID_TAG_ID(HttpStatus.BAD_REQUEST, "PROMPT-021", "태그 식별자가 유효하지 않습니다."),
    INVALID_IMAGE_UPLOADER_ID(HttpStatus.BAD_REQUEST, "PROMPT-022", "이미지 업로더 식별자가 유효하지 않습니다."),
    INVALID_IMAGE_OBJECT_KEY(HttpStatus.BAD_REQUEST, "PROMPT-023", "이미지 Object Key가 유효하지 않습니다."),
    INVALID_IMAGE_FILE_NAME(HttpStatus.BAD_REQUEST, "PROMPT-024", "이미지 파일명이 유효하지 않습니다."),
    UNSUPPORTED_IMAGE_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "PROMPT-025", "지원하지 않는 이미지 형식입니다."),
    INVALID_IMAGE_FILE_SIZE(HttpStatus.BAD_REQUEST, "PROMPT-026", "이미지 파일 크기가 유효하지 않습니다."),
    INVALID_IMAGE_DIMENSIONS(HttpStatus.BAD_REQUEST, "PROMPT-027", "이미지 크기가 유효하지 않습니다."),
    INVALID_IMAGE_STATUS_TRANSITION(HttpStatus.CONFLICT, "PROMPT-028", "이미지 처리 상태를 변경할 수 없습니다."),
    INVALID_IMAGE_PROCESSING_VERSION(HttpStatus.BAD_REQUEST, "PROMPT-029", "워터마크 처리 버전이 유효하지 않습니다."),
    INVALID_IMAGE_FAILURE_CODE(HttpStatus.BAD_REQUEST, "PROMPT-030", "이미지 처리 실패 코드가 유효하지 않습니다."),
    IMAGE_NOT_OWNED(HttpStatus.FORBIDDEN, "PROMPT-031", "본인이 업로드한 이미지만 사용할 수 있습니다."),
    IMAGE_NOT_READY(HttpStatus.CONFLICT, "PROMPT-032", "워터마크 처리가 완료되지 않은 이미지입니다."),
    IMAGE_ALREADY_ATTACHED(HttpStatus.CONFLICT, "PROMPT-033", "이미 사용 중인 이미지입니다."),
    INVALID_IMAGE_METADATA(HttpStatus.BAD_REQUEST, "PROMPT-034", "이미지 메타데이터가 유효하지 않습니다."),
    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "PROMPT-035", "이미지 자산을 찾을 수 없습니다."),
    IMAGE_UPLOAD_NOT_FOUND(HttpStatus.CONFLICT, "PROMPT-036", "S3에 업로드된 이미지가 없습니다."),
    IMAGE_UPLOAD_METADATA_MISMATCH(HttpStatus.BAD_REQUEST, "PROMPT-037", "업로드된 이미지 정보가 요청과 일치하지 않습니다."),
    IMAGE_STORAGE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "PROMPT-038", "이미지 저장소를 사용할 수 없습니다."),
    INVALID_IMAGE_UPLOAD_METADATA(HttpStatus.BAD_REQUEST, "PROMPT-039", "이미지 업로드 완료 정보가 유효하지 않습니다."),
    INVALID_IMAGE_UPLOAD_COUNT(HttpStatus.BAD_REQUEST, "PROMPT-040", "업로드할 이미지 개수가 유효하지 않습니다."),
    INVALID_IMAGE_WATERMARK_JOB(HttpStatus.BAD_REQUEST, "PROMPT-041", "워터마크 작업 정보가 유효하지 않습니다."),
    IMAGE_ORIGINAL_DOWNLOAD_FAILED(
            HttpStatus.SERVICE_UNAVAILABLE,
            "PROMPT-042",
            "원본 이미지를 불러올 수 없습니다."
    ),
    INVALID_IMAGE_SOURCE(HttpStatus.CONFLICT, "PROMPT-043", "처리할 원본 이미지가 유효하지 않습니다."),
    IMAGE_WATERMARK_RENDER_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "PROMPT-044",
            "이미지 워터마크 처리에 실패했습니다."
    ),
    IMAGE_WATERMARK_UPLOAD_FAILED(
            HttpStatus.SERVICE_UNAVAILABLE,
            "PROMPT-045",
            "워터마크 이미지를 저장할 수 없습니다."
    ),
    IMAGE_PROCESSING_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "PROMPT-046",
            "이미지 처리 중 오류가 발생했습니다."
    ),
    INVALID_PROMPT_VISIBILITY(HttpStatus.BAD_REQUEST, "PROMPT-047", "프롬프트 공개 범위가 유효하지 않습니다."),
    DUPLICATE_IMAGE(HttpStatus.CONFLICT, "PROMPT-048", "동일한 이미지를 중복으로 연결할 수 없습니다."),
    DUPLICATE_IMAGE_ORDER(HttpStatus.CONFLICT, "PROMPT-049", "이미지 정렬 순서는 중복될 수 없습니다."),
    INVALID_PROMPT_DESCRIPTION(HttpStatus.BAD_REQUEST, "PROMPT-050", "프롬프트 설명이 유효하지 않습니다."),
    REQUIRED_TAG_MISSING(HttpStatus.BAD_REQUEST, "PROMPT-051", "필수 태그를 하나 이상 선택해야 합니다."),
    IMAGE_REQUIRED(HttpStatus.BAD_REQUEST, "PROMPT-052", "프롬프트 이미지를 하나 이상 선택해야 합니다."),
    INVALID_IMAGE_STATUS_QUERY_COUNT(HttpStatus.BAD_REQUEST, "PROMPT-053", "조회할 이미지 개수가 유효하지 않습니다."),
    DUPLICATE_IMAGE_ID(HttpStatus.BAD_REQUEST, "PROMPT-054", "중복된 이미지 식별자는 조회할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    PromptErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
