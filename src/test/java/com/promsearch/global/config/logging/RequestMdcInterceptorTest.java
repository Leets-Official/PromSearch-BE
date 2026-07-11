package com.promsearch.global.config.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.servlet.HandlerMapping;

class RequestMdcInterceptorTest {

    private final RequestMdcInterceptor interceptor = new RequestMdcInterceptor();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @DisplayName("X-Request-Id가 있으면 MDC와 응답 헤더로 전파한다")
    @Test
    void propagateRequestIdToMdcAndResponseHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/prompts/1");
        request.addHeader(RequestMdcInterceptor.REQUEST_ID_HEADER, "request-123");
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(response.getHeader(RequestMdcInterceptor.REQUEST_ID_HEADER)).isEqualTo("request-123");
        assertThat(MDC.get("requestId")).isEqualTo("request-123");
        assertThat(MDC.get("traceId")).isEqualTo("request-123");
        assertThat(MDC.get("method")).isEqualTo("GET");
        assertThat(MDC.get("path")).isEqualTo("/api/v1/prompts/1");
        assertThat(MDC.get("clientIp")).isEqualTo("10.0.0.1");
        assertThat(MDC.get("userId")).isEmpty();
        assertThat(MDC.get("memberId")).isEmpty();
        assertThat(MDC.get("userRole")).isEmpty();
    }

    @DisplayName("인증된 사용자가 있으면 userId, memberId, userRole을 MDC에 담는다")
    @Test
    void putAuthenticatedUserContextToMdc() {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(7L, "USER");
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/v1/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        interceptor.preHandle(request, response, new Object());

        assertThat(MDC.get("userId")).isEqualTo("7");
        assertThat(MDC.get("memberId")).isEqualTo("7");
        assertThat(MDC.get("userRole")).isEqualTo("USER");
    }

    @DisplayName("X-Request-Id가 없으면 생성하고 X-Forwarded-For의 첫 IP만 사용한다")
    @Test
    void generateRequestIdAndResolveForwardedClientIp() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/signup");
        request.addHeader("X-Forwarded-For", "203.0.113.7, 10.0.0.2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        interceptor.preHandle(request, response, new Object());

        assertThat(response.getHeader(RequestMdcInterceptor.REQUEST_ID_HEADER)).isNotBlank();
        assertThat(MDC.get("requestId")).isEqualTo(response.getHeader(RequestMdcInterceptor.REQUEST_ID_HEADER));
        assertThat(MDC.get("clientIp")).isEqualTo("203.0.113.7");
    }


    @DisplayName("안전하지 않은 X-Request-Id는 새 UUID로 대체한다")
    @Test
    void replaceUnsafeRequestIdWithGeneratedUuid() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/prompts/1");
        request.addHeader(RequestMdcInterceptor.REQUEST_ID_HEADER, "gildong@example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();

        interceptor.preHandle(request, response, new Object());

        String requestId = MDC.get("requestId");
        assertThat(requestId).isNotEqualTo("gildong@example.com");
        assertThat(response.getHeader(RequestMdcInterceptor.REQUEST_ID_HEADER)).isEqualTo(requestId);
        assertThat(UUID.fromString(requestId).toString()).isEqualTo(requestId);
        assertThat(MDC.get("traceId")).isEqualTo(requestId);
    }

    @DisplayName("안전하지 않은 client IP 헤더 값은 MDC와 완료 로그에 남기지 않는다")
    @Test
    void unsafeClientIpHeadersDoNotRemainInMdcOrCompletionLog() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(RequestMdcInterceptor.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/prompts/1");
            request.addHeader("X-Forwarded-For", "Bearer secret-token");
            request.addHeader("X-Real-IP", "203.0.113.7\r\nBearer other-secret");
            request.setRemoteAddr("198.51.100.10");
            MockHttpServletResponse response = new MockHttpServletResponse();

            interceptor.preHandle(request, response, new Object());
            interceptor.afterCompletion(request, response, new Object(), null);

            assertThat(appender.list).hasSize(1);
            Map<String, String> mdc = appender.list.getFirst().getMDCPropertyMap();
            assertThat(mdc).containsEntry("clientIp", "198.51.100.10");

            String serializedLogSurface = appender.list.getFirst().getFormattedMessage() + " "
                    + String.join(" ", mdc.keySet()) + " " + String.join(" ", mdc.values());
            assertThat(serializedLogSurface)
                    .doesNotContain("Bearer secret-token")
                    .doesNotContain("Bearer other-secret")
                    .doesNotContain("\r")
                    .doesNotContain("\n");
        } finally {
            logger.detachAppender(appender);
        }
    }

    @DisplayName("요청 완료 시 status, duration, uriTemplate, exception을 MDC에 담아 한 줄 로그를 남기고 interceptor 소유 MDC만 제거한다")
    @Test
    void logCompletionMdcAndClear() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(RequestMdcInterceptor.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/prompts/42");
            request.addHeader(RequestMdcInterceptor.REQUEST_ID_HEADER, "request-456");
            request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/v1/prompts/{promptId}");
            MockHttpServletResponse response = new MockHttpServletResponse();
            response.setStatus(503);

            interceptor.preHandle(request, response, new Object());
            request.setAttribute(RequestMdcInterceptor.START_TIME_ATTRIBUTE, System.nanoTime() - 2_000_000L);

            interceptor.afterCompletion(request, response, new Object(), new IllegalStateException("failed"));

            assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
            assertThat(appender.list).hasSize(1);

            ILoggingEvent event = appender.list.getFirst();
            Map<String, String> mdc = event.getMDCPropertyMap();
            assertThat(event.getLevel()).isEqualTo(Level.INFO);
            assertThat(event.getFormattedMessage()).isEqualTo("http_request_completed");
            assertThat(mdc).containsEntry("requestId", "request-456")
                    .containsEntry("traceId", "request-456")
                    .containsEntry("method", "GET")
                    .containsEntry("path", "/api/v1/prompts/42")
                    .containsEntry("uriTemplate", "/api/v1/prompts/{promptId}")
                    .containsEntry("statusCode", "503")
                    .containsEntry("event", "http.request.completed")
                    .containsEntry("exception", "IllegalStateException");
            assertThat(Long.parseLong(mdc.get("durationMs"))).isGreaterThanOrEqualTo(0L);
        } finally {
            logger.detachAppender(appender);
        }
    }

    @DisplayName("요청 완료 후 interceptor가 소유하지 않은 기존 MDC key는 보존한다")
    @Test
    void preservePreExistingMdcKeysAfterCompletion() throws Exception {
        MDC.put("externalKey", "external-value");
        MDC.put("requestId", "previous-request-id");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/prompts/42");
        request.addHeader(RequestMdcInterceptor.REQUEST_ID_HEADER, "request-owned");
        MockHttpServletResponse response = new MockHttpServletResponse();

        interceptor.preHandle(request, response, new Object());
        interceptor.afterCompletion(request, response, new Object(), null);

        assertThat(MDC.get("externalKey")).isEqualTo("external-value");
        assertThat(MDC.get("requestId")).isEqualTo("previous-request-id");
        assertThat(MDC.get("traceId")).isNull();
        assertThat(MDC.get("statusCode")).isNull();
    }

    @DisplayName("요청 완료 로그와 MDC에는 Authorization, token, raw password/body/full email 같은 민감정보를 남기지 않는다")
    @Test
    void completionLogAndMdcDoNotContainSensitiveRequestData() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(RequestMdcInterceptor.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/signup");
            request.addHeader(RequestMdcInterceptor.REQUEST_ID_HEADER, "request-sensitive");
            request.addHeader("Authorization", "Bearer secret-token-value");
            request.addHeader("X-Api-Token", "token-like-secret");
            request.setContentType("application/json");
            request.setContent("{\"email\":\"gildong@example.com\",\"password\":\"raw-password-123\"}"
                    .getBytes(StandardCharsets.UTF_8));
            MockHttpServletResponse response = new MockHttpServletResponse();
            response.setStatus(201);

            interceptor.preHandle(request, response, new Object());
            interceptor.afterCompletion(request, response, new Object(), null);

            assertThat(appender.list).hasSize(1);
            ILoggingEvent event = appender.list.getFirst();
            Map<String, String> mdc = event.getMDCPropertyMap();

            assertThat(event.getFormattedMessage()).isEqualTo("http_request_completed");
            assertThat(mdc.keySet()).isSubsetOf(Set.of(
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
            ));

            String serializedLogSurface = event.getFormattedMessage() + " " + String.join(" ", mdc.keySet())
                    + " " + String.join(" ", mdc.values());
            assertThat(serializedLogSurface)
                    .doesNotContain("Authorization")
                    .doesNotContain("Bearer secret-token-value")
                    .doesNotContain("X-Api-Token")
                    .doesNotContain("token-like-secret")
                    .doesNotContain("raw-password-123")
                    .doesNotContain("gildong@example.com")
                    .doesNotContain("requestBody")
                    .doesNotContain("password");
        } finally {
            logger.detachAppender(appender);
        }
    }

}
