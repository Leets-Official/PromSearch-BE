package com.promsearch.global.config.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPatternParser;

class WebMvcConfigTest {

    private final PathPatternParser parser = new PathPatternParser();

    @DisplayName("Swagger/docs, health-check, error, static noise 경로를 요청 로깅에서 제외한다")
    @Test
    void requestLoggingExcludePatterns() {
        assertThat(isExcluded("/docs")).isTrue();
        assertThat(isExcluded("/docs-json")).isTrue();
        assertThat(isExcluded("/swagger-ui/index.html")).isTrue();
        assertThat(isExcluded("/test/health-check")).isTrue();
        assertThat(isExcluded("/error")).isTrue();
        assertThat(isExcluded("/favicon.ico")).isTrue();
        assertThat(isExcluded("/static/app.css")).isTrue();

        assertThat(isExcluded("/api/v1/prompts")).isFalse();
        assertThat(isExcluded("/api/v1/auth/signup")).isFalse();
    }

    private boolean isExcluded(String path) {
        PathContainer pathContainer = PathContainer.parsePath(path);
        return WebMvcConfig.REQUEST_LOGGING_EXCLUDE_PATTERNS.stream()
                .map(parser::parse)
                .anyMatch(pattern -> pattern.matches(pathContainer));
    }
}
