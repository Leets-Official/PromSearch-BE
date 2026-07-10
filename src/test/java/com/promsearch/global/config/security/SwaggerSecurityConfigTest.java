package com.promsearch.global.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class SwaggerSecurityConfigTest {

    private final SwaggerSecurityConfig config = new SwaggerSecurityConfig();
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @DisplayName("dev/prod에서 Swagger 활성화 시 Basic Auth username과 password가 모두 필요하다")
    @Test
    void swaggerCredentialRequiredWhenEnabled() {
        assertThatThrownBy(() -> config.swaggerUserDetailsService("", "password", passwordEncoder))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SWAGGER_AUTH_USERNAME")
                .hasMessageContaining("SWAGGER_AUTH_PASSWORD");

        assertThatThrownBy(() -> config.swaggerUserDetailsService("swagger", " ", passwordEncoder))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SWAGGER_AUTH_USERNAME")
                .hasMessageContaining("SWAGGER_AUTH_PASSWORD");
    }

    @DisplayName("Swagger Basic Auth 사용자를 InMemory 사용자로 구성한다")
    @Test
    void swaggerUserDetailsService() {
        UserDetails user = config.swaggerUserDetailsService("swagger", "secret", passwordEncoder)
                .loadUserByUsername("swagger");

        assertThat(user.getUsername()).isEqualTo("swagger");
        assertThat(passwordEncoder.matches("secret", user.getPassword())).isTrue();
        assertThat(user.getAuthorities()).extracting("authority").contains("ROLE_SWAGGER");
    }
}
