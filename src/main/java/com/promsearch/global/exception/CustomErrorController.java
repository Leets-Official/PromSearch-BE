package com.promsearch.global.exception;

import com.promsearch.global.exception.constant.CommonErrorCode;
import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.response.code.BaseCode;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/error")
public class CustomErrorController implements ErrorController {

    private static final Logger log = LoggerFactory.getLogger(CustomErrorController.class);

    @RequestMapping
    public ResponseEntity<ApiResponse<Object>> handleError(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        Throwable exception = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        // 필터/서블릿 레벨에서 BusinessException이 감싸져 넘어온 경우 원래 에러 코드를 유지한다.
        if (exception != null) {
            Throwable cause = getRootCause(exception);
            if (cause instanceof BusinessException businessException) {
                BaseCode code = businessException.getBaseCode();
                log.warn("[ERROR CONTROLLER - BusinessException] uri={}, code={}, message={}",
                        requestUri, code.getCode(), code.getMessage());

                return ResponseEntity
                        .status(code.getHttpStatus())
                        .body(ApiResponse.onFailure(code.getCode(), code.getMessage(), null));
            }
        }

        // Spring 기본 /error 요청은 HTTP 상태값을 공통 에러 코드로 변환한다.
        HttpStatus status = HttpStatus.resolve(statusCode != null ? statusCode : 500);
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        log.warn("[ERROR CONTROLLER] status={}, uri={}", statusCode, requestUri);

        CommonErrorCode errorCode = resolveErrorCode(status);
        ApiResponse<Object> body = ApiResponse.onFailure(errorCode.getCode(), errorCode.getMessage(), null);

        return ResponseEntity.status(status).body(body);
    }

    // 래핑된 예외의 가장 안쪽 원인을 찾는다.
    private Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

    // HTTP 상태값을 프로젝트 공통 에러 코드로 매핑한다.
    private CommonErrorCode resolveErrorCode(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> CommonErrorCode.NOT_FOUND;
            case BAD_REQUEST -> CommonErrorCode.BAD_REQUEST;
            case UNAUTHORIZED -> CommonErrorCode.UNAUTHORIZED;
            case FORBIDDEN -> CommonErrorCode.FORBIDDEN;
            case METHOD_NOT_ALLOWED -> CommonErrorCode.METHOD_NOT_ALLOWED;
            default -> CommonErrorCode.INTERNAL_SERVER_ERROR;
        };
    }
}
