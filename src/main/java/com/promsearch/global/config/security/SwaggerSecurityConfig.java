package com.promsearch.global.config.security;

import com.promsearch.global.security.ApiAccessDeniedHandler;
import com.promsearch.global.security.ApiAuthenticationEntryPoint;
import com.promsearch.global.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

@Configuration
@EnableWebSecurity
public class SwaggerSecurityConfig {

    private static final String[] SWAGGER_PATHS = {
            "/docs", "/docs/**",
            "/docs-json", "/docs-json/**",
            "/v3/api-docs", "/v3/api-docs/**",
            "/swagger-ui.html", "/swagger-ui/**"
    };

    @Bean
    @Order(1)
    @Profile({"dev", "prod"})
    @ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true")
    public SecurityFilterChain swaggerDocsSecurityFilterChain(HttpSecurity http) throws Exception {
        /* dev/prod에서 Swagger 경로만 Basic Auth로 막고 일반 API 보안 정책과 분리한다. */
        return http
                .securityMatcher(SWAGGER_PATHS)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    @Order(2)
    @ConditionalOnBean({
            JwtAuthenticationFilter.class,
            ApiAuthenticationEntryPoint.class,
            ApiAccessDeniedHandler.class
    })
    public SecurityFilterChain applicationSecurityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ApiAuthenticationEntryPoint apiAuthenticationEntryPoint,
            ApiAccessDeniedHandler apiAccessDeniedHandler
    ) throws Exception {
        /* Swagger Basic Auth와 분리해 일반 API는 stateless JWT로 보호한다. */
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(apiAuthenticationEntryPoint)
                        .accessDeniedHandler(apiAccessDeniedHandler))
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(SWAGGER_PATHS).permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/prompts/*/comments",
                                "/api/v1/comments/*/replies"
                        ).permitAll()
                        .requestMatchers(
                                "/test/health-check",
                                "/api/v1/auth/signup",
                                "/api/v1/auth/login",
                                "/api/v1/auth/reissue",
                                "/api/v1/auth/oauth/**",
                                "/api/v1/auth/swagger-token",
                                /*
                                 * 홈 목록, 태그 목록, 상대 프로필은 서비스 탐색 흐름의 공개 조회 API입니다.
                                 * 인증 토큰이 있으면 필터가 SecurityContext를 채워 liked/bookmarked 같은 개인화 필드를 만들 수 있고,
                                 * 토큰이 없으면 비회원용 응답으로 내려갑니다.
                                 */
                                "/api/v1/home/**",
                                "/api/v1/tags",
                                "/api/v1/users/*/profile",
                                "/api/v1/users/nicknames/availability",
                                "/error"
                        ).permitAll()
                        .requestMatchers(RegexRequestMatcher.regexMatcher(
                                HttpMethod.GET,
                                "^/api/v1/prompts/(?!draft$)[^/]+$")).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    @ConditionalOnBean(JwtAuthenticationFilter.class)
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(jwtAuthenticationFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    @Profile({"dev", "prod"})
    @ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true")
    public UserDetailsService swaggerUserDetailsService(
            @Value("${SWAGGER_AUTH_USERNAME:}") String username,
            @Value("${SWAGGER_AUTH_PASSWORD:}") String password,
            PasswordEncoder passwordEncoder
    ) {
        /* 문서를 외부 환경에서 켤 때는 빈 credential로 서버가 뜨지 않게 즉시 실패시킨다. */
        validateSwaggerCredential(username, password);

        return new InMemoryUserDetailsManager(User.withUsername(username)
                .password(passwordEncoder.encode(password))
                .roles("SWAGGER")
                .build());
    }

    @Bean
    @ConditionalOnMissingBean(UserDetailsService.class)
    public UserDetailsService emptyUserDetailsService() {
        return new InMemoryUserDetailsManager();
    }

    private void validateSwaggerCredential(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "SWAGGER_AUTH_USERNAME and SWAGGER_AUTH_PASSWORD are required when Swagger is enabled in dev/prod profiles."
            );
        }
    }
}
