package com.promsearch.global.config.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.promsearch.global.security.ApiAccessDeniedHandler;
import com.promsearch.global.security.ApiAuthenticationEntryPoint;

@SpringBootTest
@AutoConfigureMockMvc
@Import({SecurityRequestLoggingTest.TestSecurityConfig.class, SecurityRequestLoggingTest.TestController.class})
class SecurityRequestLoggingTest {

    @Autowired
    private MockMvc mockMvc;

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @DisplayName("인증 없이 보호 API를 호출한 401 응답도 요청 완료 로그에 남긴다")
    @Test
    void logAuthenticationEntryPointResponse() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());

        assertSecurityLog("401");
    }

    @DisplayName("잘못된 Bearer 토큰을 JWT 필터가 거절한 401 응답도 요청 완료 로그에 남긴다")
    @Test
    void logJwtFilterRejection() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Invalid token"))
                .andExpect(status().isUnauthorized());

        assertSecurityLog("401");
    }

    @DisplayName("권한이 부족해 AccessDeniedHandler가 만든 403도 요청 완료 로그에 남긴다")
    @Test
    void logAccessDeniedHandlerResponse() throws Exception {
        mockMvc.perform(get("/test/logging/admin-only")
                        .with(user("member").roles("USER")))
                .andExpect(status().isForbidden());

        assertLog("/test/logging/admin-only", "GET", "403", Level.WARN);
    }

    @DisplayName("Controller 예외가 공통 예외 처리로 변환된 500도 요청 완료 로그에 남긴다")
    @Test
    void logHandledControllerFailure() throws Exception {
        mockMvc.perform(get("/test/logging/failure")
                        .with(user("member").roles("USER")))
                .andExpect(status().isInternalServerError());

        assertLog("/test/logging/failure", "GET", "500", Level.ERROR);
    }

    private void assertSecurityLog(String expectedStatus) {
        assertLog("/api/v1/users/me", "PATCH", expectedStatus, Level.WARN);
    }

    private void assertLog(String path, String method, String expectedStatus, Level expectedLevel) {
        ILoggingEvent event = appender.list.stream()
                .filter(candidate -> "http_request_completed".equals(candidate.getFormattedMessage()))
                .filter(candidate -> path.equals(candidate.getMDCPropertyMap().get("path")))
                .findFirst()
                .orElseThrow();
        Map<String, String> mdc = event.getMDCPropertyMap();

        assertThat(event.getLevel()).isEqualTo(expectedLevel);
        assertThat(mdc)
                .containsEntry("statusCode", expectedStatus)
                .containsEntry("event", "http.request.completed")
                .containsEntry("method", method);
        assertThat(mdc.get("requestId")).isNotBlank();
        assertThat(mdc.get("durationMs")).isNotBlank();
    }

    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        @Order(0)
        SecurityFilterChain loggingTestSecurityFilterChain(
                HttpSecurity http,
                ApiAuthenticationEntryPoint authenticationEntryPoint,
                ApiAccessDeniedHandler accessDeniedHandler
        ) throws Exception {
            return http
                    .securityMatcher("/test/logging/**")
                    .csrf(csrf -> csrf.disable())
                    .exceptionHandling(exception -> exception
                            .authenticationEntryPoint(authenticationEntryPoint)
                            .accessDeniedHandler(accessDeniedHandler))
                    .authorizeHttpRequests(authorize -> authorize
                            .requestMatchers("/test/logging/admin-only").hasRole("ADMIN")
                            .anyRequest().authenticated())
                    .build();
        }
    }

    @RestController
    static class TestController {

        @GetMapping("/test/logging/admin-only")
        String adminOnly() {
            return "ok";
        }

        @GetMapping("/test/logging/failure")
        String failure() {
            throw new IllegalStateException("logging test failure");
        }
    }
}
