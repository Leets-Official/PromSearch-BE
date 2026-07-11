package com.promsearch.global.config.logging;

import com.promsearch.global.security.AuthenticatedUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

@Component
public class RequestMdcInterceptor implements HandlerInterceptor {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String START_TIME_ATTRIBUTE = RequestMdcInterceptor.class.getName() + ".START_TIME";

    private static final String MDC_SNAPSHOT_ATTRIBUTE = RequestMdcInterceptor.class.getName() + ".MDC_SNAPSHOT";
    private static final Set<String> OWNED_MDC_KEYS = Set.of(
            "requestId",
            "traceId",
            "method",
            "path",
            "clientIp",
            "userId",
            "memberId",
            "userRole",
            "event",
            "uriTemplate",
            "statusCode",
            "durationMs",
            "exception"
    );

    private static final Logger log = LoggerFactory.getLogger(RequestMdcInterceptor.class);
    private static final String EMPTY_USER_ID = "";
    private static final int MAX_REQUEST_ID_LENGTH = 64;
    private static final int MAX_CLIENT_IP_LENGTH = 45;
    private static final Pattern SAFE_REQUEST_ID_PATTERN = Pattern.compile("[A-Za-z0-9._:-]+");
    private static final Pattern SAFE_CLIENT_IP_PATTERN = Pattern.compile("[A-Fa-f0-9:.%\\[\\]-]+");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestId = resolveRequestId(request);
        /* 다른 필터나 쓰레드 컨텍스트가 넣은 MDC 값을 요청 종료 후 되돌리기 위해 저장한다. */
        request.setAttribute(MDC_SNAPSHOT_ATTRIBUTE, MDC.getCopyOfContextMap());
        request.setAttribute(START_TIME_ATTRIBUTE, System.nanoTime());
        response.setHeader(REQUEST_ID_HEADER, requestId);

        MDC.put("requestId", requestId);
        MDC.put("traceId", requestId);
        MDC.put("method", request.getMethod());
        MDC.put("path", request.getRequestURI());
        MDC.put("clientIp", resolveClientIp(request));
        putAuthenticatedUserContext();
        MDC.put("event", "http.request");
        return true;
    }

    private void putAuthenticatedUserContext() {
        MDC.put("userId", EMPTY_USER_ID);
        MDC.put("memberId", EMPTY_USER_ID);
        MDC.put("userRole", "");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }
        if (authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal) {
            String userId = String.valueOf(principal.userId());
            MDC.put("userId", userId);
            MDC.put("memberId", userId);
            MDC.put("userRole", principal.role() == null ? "" : principal.role());
        }
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            @Nullable Exception exception
    ) {
        try {
            MDC.put("uriTemplate", resolveUriTemplate(request));
            MDC.put("statusCode", String.valueOf(response.getStatus()));
            MDC.put("durationMs", String.valueOf(resolveDurationMs(request)));
            MDC.put("event", "http.request.completed");
            MDC.put("exception", resolveExceptionName(exception));

            log.info("http_request_completed");
        } finally {
            restorePreviousMdc(request);
        }
    }

    private void restorePreviousMdc(HttpServletRequest request) {
        Object snapshot = request.getAttribute(MDC_SNAPSHOT_ATTRIBUTE);
        if (snapshot instanceof Map<?, ?> previousMdc) {
            /* 기존 MDC가 있던 요청은 원래 상태로 복구해 다음 로그에 값이 섞이지 않게 한다. */
            MDC.setContextMap(castMdcSnapshot(previousMdc));
            return;
        }

        /* 기존 MDC가 없던 요청은 이 interceptor가 넣은 key만 정리한다. */
        OWNED_MDC_KEYS.forEach(MDC::remove);
    }

    private Map<String, String> castMdcSnapshot(Map<?, ?> snapshot) {
        return snapshot.entrySet().stream()
                .filter(entry -> entry.getKey() instanceof String && entry.getValue() instanceof String)
                .collect(Collectors.toMap(
                        entry -> (String) entry.getKey(),
                        entry -> (String) entry.getValue()
                ));
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        String sanitizedRequestId = sanitizeRequestId(requestId);
        return sanitizedRequestId.isEmpty() ? UUID.randomUUID().toString() : sanitizedRequestId;
    }

    private String sanitizeRequestId(@Nullable String requestId) {
        if (requestId == null) {
            return "";
        }

        /* request id는 로그 필드에 그대로 남으므로 길이와 문자 집합을 제한한다. */
        String sanitized = removeLineBreaks(requestId).trim();
        if (sanitized.isEmpty() || sanitized.length() > MAX_REQUEST_ID_LENGTH) {
            return "";
        }

        return SAFE_REQUEST_ID_PATTERN.matcher(sanitized).matches() ? sanitized : "";
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        String forwardedClientIp = firstForwardedIp(forwardedFor);
        String sanitizedForwardedClientIp = sanitizeClientIp(forwardedClientIp);
        if (!sanitizedForwardedClientIp.isEmpty()) {
            return sanitizedForwardedClientIp;
        }

        String sanitizedRealIp = sanitizeClientIp(request.getHeader("X-Real-IP"));
        if (!sanitizedRealIp.isEmpty()) {
            return sanitizedRealIp;
        }

        return sanitizeClientIp(request.getRemoteAddr());
    }

    private String firstForwardedIp(@Nullable String forwardedFor) {
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return "";
        }
        return forwardedFor.split(",", 2)[0];
    }

    private String sanitizeClientIp(@Nullable String clientIp) {
        if (clientIp == null) {
            return "";
        }

        /* IP 헤더 오염으로 토큰이나 개행 문자가 로그에 섞이지 않게 허용 문자만 남긴다. */
        String sanitized = removeLineBreaks(clientIp).trim();
        if (sanitized.isEmpty() || sanitized.length() > MAX_CLIENT_IP_LENGTH) {
            return "";
        }

        return SAFE_CLIENT_IP_PATTERN.matcher(sanitized).matches() ? sanitized : "";
    }

    private String removeLineBreaks(String value) {
        return value.replace("\r", "").replace("\n", "");
    }

    private String resolveUriTemplate(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern instanceof String uriTemplate && !uriTemplate.isBlank()) {
            return uriTemplate;
        }
        return request.getRequestURI();
    }

    private long resolveDurationMs(HttpServletRequest request) {
        Object startTime = request.getAttribute(START_TIME_ATTRIBUTE);
        if (startTime instanceof Long startedAtNanos) {
            return Math.max(0L, (System.nanoTime() - startedAtNanos) / 1_000_000L);
        }
        return 0L;
    }

    private String resolveExceptionName(@Nullable Exception exception) {
        return exception == null ? "" : exception.getClass().getSimpleName();
    }
}
