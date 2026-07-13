package com.promsearch.global.config.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.ServletException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @DisplayName("필터 체인 전체에 requestId와 요청 정보를 전파하고 완료 후 MDC를 정리한다")
    @Test
    void propagateRequestContextAndRestoreMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/prompts/1");
        request.addHeader(RequestLoggingFilter.REQUEST_ID_HEADER, "request-123");
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            assertThat(MDC.get("requestId")).isEqualTo("request-123");
            assertThat(MDC.get("traceId")).isEqualTo("request-123");
            assertThat(MDC.get("method")).isEqualTo("GET");
            assertThat(MDC.get("path")).isEqualTo("/api/v1/prompts/1");
            assertThat(MDC.get("clientIp")).isEqualTo("10.0.0.1");
        });

        assertThat(response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER)).isEqualTo("request-123");
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @DisplayName("JWT 인증이 성공하면 사용자 식별 정보를 요청 MDC에 보강한다")
    @Test
    void putAuthenticatedUserContext() throws Exception {
        ListAppender<ILoggingEvent> appender = attachAppender();
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/v1/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            filter.doFilter(request, response, (servletRequest, servletResponse) -> {
                RequestLoggingFilter.putAuthenticatedUserContext(request, 7L, "USER");
                assertThat(MDC.get("userId")).isEqualTo("7");
                assertThat(MDC.get("memberId")).isEqualTo("7");
                assertThat(MDC.get("userRole")).isEqualTo("USER");
            });

            assertThat(appender.list.getFirst().getMDCPropertyMap())
                    .containsEntry("userId", "7")
                    .containsEntry("memberId", "7")
                    .containsEntry("userRole", "USER");
        } finally {
            detachAppender(appender);
        }
    }

    @DisplayName("X-Request-Id가 없거나 안전하지 않으면 새 UUID를 발급한다")
    @Test
    void replaceMissingOrUnsafeRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/prompts/1");
        request.addHeader(RequestLoggingFilter.REQUEST_ID_HEADER, "gildong@example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String requestId = response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER);
        assertThat(requestId).isNotEqualTo("gildong@example.com");
        assertThat(UUID.fromString(requestId).toString()).isEqualTo(requestId);
    }

    @DisplayName("Security에서 종료된 401과 403도 WARN 완료 로그로 기록한다")
    @Test
    void logSecurityResponseCompleted() throws Exception {
        ListAppender<ILoggingEvent> appender = attachAppender();

        try {
            MockHttpServletRequest unauthorizedRequest =
                    new MockHttpServletRequest("PATCH", "/api/v1/users/me");
            unauthorizedRequest.setAttribute(
                    HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
                    "/api/v1/users/me"
            );
            MockHttpServletResponse unauthorizedResponse = new MockHttpServletResponse();

            filter.doFilter(unauthorizedRequest, unauthorizedResponse,
                    (request, response) -> ((MockHttpServletResponse) response).setStatus(401));

            MockHttpServletRequest forbiddenRequest =
                    new MockHttpServletRequest("DELETE", "/api/v1/users/me");
            MockHttpServletResponse forbiddenResponse = new MockHttpServletResponse();

            filter.doFilter(forbiddenRequest, forbiddenResponse,
                    (request, response) -> ((MockHttpServletResponse) response).setStatus(403));

            assertThat(appender.list).hasSize(2);
            assertSecurityEvent(appender.list.get(0), "401", "PATCH");
            assertSecurityEvent(appender.list.get(1), "403", "DELETE");
        } finally {
            detachAppender(appender);
        }
    }

    @DisplayName("하위 체인이 예외를 던지면 500과 예외 종류를 ERROR 완료 로그로 남긴다")
    @Test
    void logUnhandledFailure() {
        ListAppender<ILoggingEvent> appender = attachAppender();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/prompts/1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            assertThatThrownBy(() -> filter.doFilter(request, response, (servletRequest, servletResponse) -> {
                throw new ServletException("failed");
            })).isInstanceOf(ServletException.class);

            ILoggingEvent event = appender.list.getFirst();
            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
            assertThat(event.getMDCPropertyMap())
                    .containsEntry("statusCode", "500")
                    .containsEntry("exception", "ServletException");
        } finally {
            detachAppender(appender);
        }
    }

    @DisplayName("Error가 전파돼도 500과 오류 종류를 ERROR 완료 로그로 남긴다")
    @Test
    void logJvmError() {
        ListAppender<ILoggingEvent> appender = attachAppender();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/prompts/1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            assertThatThrownBy(() -> filter.doFilter(request, response, (servletRequest, servletResponse) -> {
                throw new AssertionError("failed");
            })).isInstanceOf(AssertionError.class);

            ILoggingEvent event = appender.list.getFirst();
            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
            assertThat(event.getMDCPropertyMap())
                    .containsEntry("statusCode", "500")
                    .containsEntry("exception", "AssertionError");
        } finally {
            detachAppender(appender);
        }
    }

    @DisplayName("기존 MDC는 요청 완료 후 원래 값으로 복원한다")
    @Test
    void preservePreExistingMdc() throws Exception {
        MDC.put("externalKey", "external-value");
        MDC.put("requestId", "previous-request-id");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/prompts/42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(MDC.get("externalKey")).isEqualTo("external-value");
        assertThat(MDC.get("requestId")).isEqualTo("previous-request-id");
        assertThat(MDC.get("traceId")).isNull();
    }

    @DisplayName("Authorization, token, password, body 같은 민감정보는 완료 로그에 남기지 않는다")
    @Test
    void completionLogDoesNotContainSensitiveRequestData() throws Exception {
        ListAppender<ILoggingEvent> appender = attachAppender();

        try {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/signup");
            request.addHeader("Authorization", "Bearer secret-token-value");
            request.addHeader("X-Api-Token", "token-like-secret");
            request.setContentType("application/json");
            request.setContent("{\"email\":\"gildong@example.com\",\"password\":\"raw-password-123\"}"
                    .getBytes(StandardCharsets.UTF_8));
            MockHttpServletResponse response = new MockHttpServletResponse();
            response.setStatus(201);

            filter.doFilter(request, response, new MockFilterChain());

            ILoggingEvent event = appender.list.getFirst();
            Map<String, String> mdc = event.getMDCPropertyMap();
            assertThat(mdc.keySet()).isSubsetOf(Set.of(
                    "requestId", "traceId", "method", "path", "clientIp",
                    "userId", "memberId", "userRole", "event", "uriTemplate",
                    "statusCode", "durationMs", "exception"
            ));

            String logSurface = event.getFormattedMessage() + " "
                    + String.join(" ", mdc.keySet()) + " "
                    + String.join(" ", mdc.values());
            assertThat(logSurface)
                    .doesNotContain("Authorization")
                    .doesNotContain("Bearer secret-token-value")
                    .doesNotContain("token-like-secret")
                    .doesNotContain("raw-password-123")
                    .doesNotContain("gildong@example.com");
        } finally {
            detachAppender(appender);
        }
    }

    @DisplayName("Swagger, health-check, 정적 리소스는 요청 완료 로그에서 제외한다")
    @Test
    void skipLowValuePaths() throws Exception {
        ListAppender<ILoggingEvent> appender = attachAppender();

        try {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, new MockFilterChain());

            assertThat(appender.list).isEmpty();
            assertThat(response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER)).isNull();
        } finally {
            detachAppender(appender);
        }
    }

    private void assertSecurityEvent(ILoggingEvent event, String status, String method) {
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage()).isEqualTo("http_request_completed");
        assertThat(event.getMDCPropertyMap())
                .containsEntry("statusCode", status)
                .containsEntry("method", method)
                .containsEntry("event", "http.request.completed");
    }

    private ListAppender<ILoggingEvent> attachAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private void detachAppender(ListAppender<ILoggingEvent> appender) {
        Logger logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
        logger.detachAppender(appender);
    }
}
