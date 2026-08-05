package com.promsearch.global.exception;

import com.promsearch.auth.domain.exception.AuthErrorCode;
import com.promsearch.global.exception.constant.CommonErrorCode;
import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.response.code.BaseCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice(annotations = RestController.class)
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    // PathVariable, RequestParam 등 단일 값 검증 실패를 처리한다.
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException e, WebRequest request) {
        String messages = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));

        log.warn("[CONSTRAINT VIOLATION] {}", messages);
        return buildResponse(e, CommonErrorCode.BAD_REQUEST, HttpHeaders.EMPTY, request, messages);
    }

    // RequestParam, PathVariable 등에 바인딩할 수 없는 값(예: 잘못된 enum 문자열)을 처리한다.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException e,
            WebRequest request
    ) {
        String message = "%s 값이 유효하지 않습니다.".formatted(e.getName());

        log.warn("[TYPE MISMATCH] {}", message);
        return buildResponse(e, CommonErrorCode.INVALID_INPUT_VALUE, HttpHeaders.EMPTY, request, message);
    }

    // @Valid Request Body 검증 실패를 필드별 메시지로 정리한다.
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        Map<String, String> errors = new LinkedHashMap<>();

        e.getBindingResult().getFieldErrors().forEach(fieldError -> {
            String fieldName = fieldError.getField();
            String errorMessage = Optional.ofNullable(fieldError.getDefaultMessage()).orElse("");
            errors.merge(fieldName, errorMessage, (oldMessage, newMessage) -> oldMessage + ", " + newMessage);
        });

        log.warn("[VALIDATION ERROR] {}", errors);
        return buildResponse(e, CommonErrorCode.INVALID_INPUT_VALUE, headers, request, errors);
    }

    // JSON 파싱 실패나 잘못된 Request Body 형식을 처리한다.
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException e,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        String detail = isProductionProfile() ? "요청 본문 형식이 올바르지 않습니다." : e.getMostSpecificCause().getMessage();

        log.warn("[MESSAGE NOT READABLE] {}", detail);
        return buildResponse(e, CommonErrorCode.INVALID_REQUEST_BODY, headers, request, detail);
    }

    // 지원하지 않는 HTTP 메서드 요청을 처리한다.
    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException e,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        log.warn("[METHOD NOT ALLOWED] {}", e.getMessage());
        return buildResponse(e, CommonErrorCode.METHOD_NOT_ALLOWED, headers, request, e.getMessage());
    }

    // 도메인/비즈니스 규칙 위반으로 의도적으로 발생시킨 예외를 처리한다.
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Object> handleBusinessException(BusinessException e, WebRequest request) {
        log.warn("[BUSINESS EXCEPTION] domain={}, code={}, message={}",
                e.getDomain(), e.getBaseCode().getCode(), e.getMessage());

        return buildResponse(e, e.getBaseCode(), HttpHeaders.EMPTY, request, null);
    }

    // 인증은 됐지만 권한(@PreAuthorize 등)이 없어 거부된 요청을 처리한다.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccessDeniedException e, WebRequest request) {
        log.warn("[ACCESS DENIED] {}", e.getMessage());

        return buildResponse(e, AuthErrorCode.FORBIDDEN, HttpHeaders.EMPTY, request, null);
    }

    // 예상하지 못한 서버 예외를 공통 500 응답으로 변환한다.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnhandledException(Exception e, WebRequest request) {
        log.error("[UNHANDLED EXCEPTION] {}", e.getMessage(), e);

        String detail = isProductionProfile()
                ? "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
                : e.getMessage();

        return buildResponse(e, CommonErrorCode.INTERNAL_SERVER_ERROR, HttpHeaders.EMPTY, request, detail);
    }

    // 모든 예외 응답을 ApiResponse 포맷으로 통일한다.
    private ResponseEntity<Object> buildResponse(
            Exception e,
            BaseCode code,
            HttpHeaders headers,
            WebRequest request,
            Object detail
    ) {
        ApiResponse<Object> body = ApiResponse.onFailure(code.getCode(), code.getMessage(), detail);
        return super.handleExceptionInternal(e, body, headers, code.getHttpStatus(), request);
    }

    private boolean isProductionProfile() {
        return "prod".equalsIgnoreCase(activeProfile);
    }
}
