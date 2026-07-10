package com.promsearch.global.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.promsearch.global.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(OutputCaptureExtension.class)
class SwaggerCredentialContextFailureTest {

    @DisplayName("dev/prod에서 Swagger 비활성화 시 Spring Security 기본 generated user/password를 만들지 않는다")
    @ParameterizedTest
    @ValueSource(strings = {"dev", "prod"})
    void disabledSwaggerDoesNotCreateGeneratedSecurityPassword(String profile, CapturedOutput output) {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        SecurityAutoConfiguration.class,
                        SecurityFilterAutoConfiguration.class,
                        UserDetailsServiceAutoConfiguration.class
                ))
                .withUserConfiguration(SwaggerSecurityConfig.class, SecurityConfig.class)
                .withPropertyValues(
                        "spring.profiles.active=" + profile,
                        "SWAGGER_ENABLE=false",
                        "springdoc.api-docs.enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(UserDetailsService.class);
                    assertThat(context.getBeanNamesForType(UserDetailsService.class))
                            .containsExactly("emptyUserDetailsService");
                    assertThatExceptionOfType(UsernameNotFoundException.class)
                            .isThrownBy(() -> context.getBean(UserDetailsService.class).loadUserByUsername("user"));
                });

        assertThat(output).doesNotContain("Using generated security password");
    }

    @DisplayName("dev/prod에서 Swagger 활성화 후 credential이 없으면 실제 Spring bean 생성 실패로 context startup이 실패한다")
    @ParameterizedTest
    @ValueSource(strings = {"dev", "prod"})
    void contextFailsWhenDevOrProdSwaggerEnabledWithoutCredentials(String profile) {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        SecurityAutoConfiguration.class,
                        SecurityFilterAutoConfiguration.class
                ))
                .withUserConfiguration(SwaggerSecurityConfig.class, SecurityConfig.class)
                .withPropertyValues(
                        "spring.profiles.active=" + profile,
                        "SWAGGER_ENABLE=true",
                        "springdoc.api-docs.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasRootCauseMessage("SWAGGER_AUTH_USERNAME and SWAGGER_AUTH_PASSWORD are required when Swagger is enabled in dev/prod profiles.");
                });
    }
}
