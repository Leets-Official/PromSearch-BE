package com.promsearch.auth.infrastructure.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.promsearch.auth.application.usecase.dto.AuthenticatedUserInfo;
import com.promsearch.auth.application.port.out.token.RefreshTokenProvider.RefreshToken;
import com.promsearch.auth.application.port.out.token.RefreshTokenProvider.RefreshTokenClaims;
import com.promsearch.auth.domain.exception.AuthDomainException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String ACCESS_SECRET = "promsearch-test-access-secret-with-at-least-32-bytes";
    private static final String REFRESH_SECRET = "promsearch-test-refresh-secret-with-at-least-32-bytes";
    private static final Instant NOW = Instant.parse("2026-07-10T00:00:00Z");

    @Test
    void createsAndParsesRefreshToken() {
        JwtTokenProvider provider = createProvider(NOW);

        RefreshToken token = provider.createRefreshToken(createUser());
        RefreshTokenClaims claims = provider.parse(token.value());

        assertThat(claims.userId()).isEqualTo(1L);
        assertThat(claims.jti()).isNotBlank();
        assertThat(claims.expiresAt()).isEqualTo(NOW.plusSeconds(120));
    }

    @Test
    void rejectsExpiredRefreshToken() {
        RefreshToken token = createProvider(NOW).createRefreshToken(createUser());
        JwtTokenProvider expiredTokenProvider = createProvider(NOW.plusSeconds(121));

        assertThatThrownBy(() -> expiredTokenProvider.parse(token.value()))
                .isInstanceOf(AuthDomainException.class);
    }

    @Test
    void rejectsAccessTokenAsRefreshToken() {
        JwtTokenProvider provider = createProvider(NOW);
        String accessToken = provider.createAccessToken(createUser());

        assertThatThrownBy(() -> provider.parse(accessToken))
                .isInstanceOf(AuthDomainException.class);
    }

    @Test
    void rejectsTamperedRefreshToken() {
        JwtTokenProvider provider = createProvider(NOW);
        String token = provider.createRefreshToken(createUser()).value();
        int signatureStart = token.lastIndexOf('.') + 1;
        char replacement = token.charAt(signatureStart) == 'A' ? 'B' : 'A';
        String tamperedToken = token.substring(0, signatureStart)
                + replacement
                + token.substring(signatureStart + 1);

        assertThatThrownBy(() -> provider.parse(tamperedToken))
                .isInstanceOf(AuthDomainException.class);
    }

    private JwtTokenProvider createProvider(Instant instant) {
        JwtProperties properties = new JwtProperties(ACCESS_SECRET, REFRESH_SECRET, 60L, 120L);
        return new JwtTokenProvider(properties, Clock.fixed(instant, ZoneOffset.UTC));
    }

    private AuthenticatedUserInfo createUser() {
        return new AuthenticatedUserInfo(
                1L,
                "gildong@example.com",
                "USER"
        );
    }
}
