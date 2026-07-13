package com.promsearch.global.config.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String ACTIVE_ATTRIBUTE = RequestLoggingFilter.class.getName() + ".ACTIVE";

    static final List<String> EXCLUDE_PATTERNS = List.of(
            "/docs", "/docs/**",
            "/docs-json", "/docs-json/**",
            "/v3/api-docs", "/v3/api-docs/**",
            "/swagger-ui.html", "/swagger-ui/**",
            "/test/health-check",
            "/actuator/health", "/actuator/health/**",
            "/health", "/health-check",
            "/error", "/error/**",
            "/favicon.ico",
            "/robots.txt",
            "/static/**", "/assets/**", "/css/**", "/js/**", "/images/**", "/webjars/**"
    );

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

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final int MAX_REQUEST_ID_LENGTH = 64;
    private static final int MAX_CLIENT_IP_LENGTH = 45;
    private static final Pattern SAFE_REQUEST_ID_PATTERN = Pattern.compile("[A-Za-z0-9._:-]+");
    private static final Pattern SAFE_CLIENT_IP_PATTERN = Pattern.compile("[A-Fa-f0-9:.%\\[\\]-]+");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Map<String, String> previousMdc = MDC.getCopyOfContextMap();
        long startedAtNanos = System.nanoTime();
        Throwable failure = null;

        request.setAttribute(ACTIVE_ATTRIBUTE, true);
        putRequestContext(request, response);

        try {
            filterChain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException exception) {
            failure = exception;
            throw exception;
        } catch (Error error) {
            failure = error;
            throw error;
        } finally {
            try {
                logCompletedRequest(request, response, startedAtNanos, failure);
            } finally {
                restorePreviousMdc(previousMdc);
            }
        }
    }

    public static void putAuthenticatedUserContext(HttpServletRequest request, long userId, @Nullable String role) {
        if (!Boolean.TRUE.equals(request.getAttribute(ACTIVE_ATTRIBUTE))) {
            return;
        }

        String userIdValue = String.valueOf(userId);
        MDC.put("userId", userIdValue);
        MDC.put("memberId", userIdValue);
        MDC.put("userRole", role == null ? "" : role);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return EXCLUDE_PATTERNS.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    private void putRequestContext(HttpServletRequest request, HttpServletResponse response) {
        String requestId = resolveRequestId(request);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        MDC.put("requestId", requestId);
        MDC.put("traceId", requestId);
        MDC.put("method", request.getMethod());
        MDC.put("path", request.getRequestURI());
        MDC.put("clientIp", resolveClientIp(request));
        MDC.put("userId", "");
        MDC.put("memberId", "");
        MDC.put("userRole", "");
        MDC.put("event", "http.request");
    }

    private void logCompletedRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            long startedAtNanos,
            @Nullable Throwable failure
    ) {
        int statusCode = resolveStatusCode(response, failure);
        MDC.put("uriTemplate", resolveUriTemplate(request));
        MDC.put("statusCode", String.valueOf(statusCode));
        MDC.put("durationMs", String.valueOf(resolveDurationMs(startedAtNanos)));
        MDC.put("event", "http.request.completed");
        MDC.put("exception", failure == null ? "" : failure.getClass().getSimpleName());

        if (statusCode >= 500) {
            log.error("http_request_completed");
        } else if (statusCode >= 400) {
            log.warn("http_request_completed");
        } else {
            log.info("http_request_completed");
        }
    }

    private int resolveStatusCode(HttpServletResponse response, @Nullable Throwable failure) {
        if (failure != null && response.getStatus() < 400) {
            return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }
        return response.getStatus();
    }

    private void restorePreviousMdc(@Nullable Map<String, String> previousMdc) {
        if (previousMdc != null) {
            MDC.setContextMap(previousMdc);
            return;
        }
        OWNED_MDC_KEYS.forEach(MDC::remove);
    }

    private String resolveRequestId(HttpServletRequest request) {
        String sanitizedRequestId = sanitizeRequestId(request.getHeader(REQUEST_ID_HEADER));
        return sanitizedRequestId.isEmpty() ? UUID.randomUUID().toString() : sanitizedRequestId;
    }

    private String sanitizeRequestId(@Nullable String requestId) {
        if (requestId == null) {
            return "";
        }

        String sanitized = removeLineBreaks(requestId).trim();
        if (sanitized.isEmpty() || sanitized.length() > MAX_REQUEST_ID_LENGTH) {
            return "";
        }

        return SAFE_REQUEST_ID_PATTERN.matcher(sanitized).matches() ? sanitized : "";
    }

    private String resolveClientIp(HttpServletRequest request) {
        String sanitizedForwardedClientIp = sanitizeClientIp(firstForwardedIp(request.getHeader("X-Forwarded-For")));
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

    private long resolveDurationMs(long startedAtNanos) {
        return Math.max(0L, (System.nanoTime() - startedAtNanos) / 1_000_000L);
    }

}
