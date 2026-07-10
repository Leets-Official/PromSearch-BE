package com.promsearch.global.config.logging;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /* 문서, 헬스체크, 정적 리소스처럼 요청 로그 가치가 낮은 경로는 제외한다. */
    static final List<String> REQUEST_LOGGING_EXCLUDE_PATTERNS = List.of(
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

    private final RequestMdcInterceptor requestMdcInterceptor;

    public WebMvcConfig(RequestMdcInterceptor requestMdcInterceptor) {
        this.requestMdcInterceptor = requestMdcInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestMdcInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(REQUEST_LOGGING_EXCLUDE_PATTERNS);
    }
}
