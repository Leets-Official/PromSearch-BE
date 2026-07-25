package com.promsearch.auth.infrastructure.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.promsearch.auth.application.usecase.dto.AuthenticatedUserInfo;
import com.promsearch.auth.application.port.out.token.IssuedRefreshToken;
import com.promsearch.auth.application.port.out.token.RefreshTokenClaims;
import com.promsearch.auth.domain.exception.AuthDomainException;
import com.promsearch.auth.domain.exception.AuthErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import org.junit.jupiter.api.Test;

class JwtTokenAdapterTest {

    private static final String ACCESS_SECRET = "promsearch-test-access-secret-with-at-least-32-bytes";
    private static final String REFRESH_SECRET = "promsearch-test-refresh-secret-with-at-least-32-bytes";
    private static final Instant NOW = Instant.parse("2026-07-10T00:00:00Z");

    @Test
    void preservesAccessTokenClaimsExpirationAndAlgorithm() {
        JwtTokenAdapter adapter = createAdapter(NOW);

        String token = adapter.issueAccessToken(createUser()).value();
        Jws<Claims> parsed = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(ACCESS_SECRET.getBytes(StandardCharsets.UTF_8)))
                .clock(() -> Date.from(NOW))
                .build()
                .parseSignedClaims(token);

        assertThat(parsed.getHeader().getAlgorithm()).isEqualTo("HS256");
        assertThat(parsed.getPayload().getSubject()).isEqualTo("1");
        assertThat(parsed.getPayload().get("userId", Long.class)).isEqualTo(1L);
        assertThat(parsed.getPayload().get("role", String.class)).isEqualTo("USER");
        assertThat(parsed.getPayload().getIssuedAt().toInstant()).isEqualTo(NOW);
        assertThat(parsed.getPayload().getExpiration().toInstant()).isEqualTo(NOW.plusSeconds(60));
        assertThat(adapter.verifyAccessToken(token).userId()).isEqualTo(1L);
        assertThat(adapter.verifyAccessToken(token).role()).isEqualTo("USER");
    }

    @Test
    void rejectsExpiredAccessTokenWithDedicatedErrorCode() {
        String token = createAdapter(NOW).issueAccessToken(createUser()).value();
        JwtTokenAdapter expiredTokenAdapter = createAdapter(NOW.plusSeconds(61));

        assertThatThrownBy(() -> expiredTokenAdapter.verifyAccessToken(token))
                .isInstanceOfSatisfying(AuthDomainException.class,
                        exception -> assertThat(exception.getBaseCode())
                                .isEqualTo(AuthErrorCode.ACCESS_TOKEN_EXPIRED));
    }

    @Test
    void rejectsMalformedAccessTokenWithInvalidTokenErrorCode() {
        JwtTokenAdapter adapter = createAdapter(NOW);

        assertThatThrownBy(() -> adapter.verifyAccessToken("not-a-jwt"))
                .isInstanceOfSatisfying(AuthDomainException.class,
                        exception -> assertThat(exception.getBaseCode())
                                .isEqualTo(AuthErrorCode.INVALID_TOKEN));
    }

    @Test
    void createsAndParsesRefreshToken() {
        JwtTokenAdapter adapter = createAdapter(NOW);

        IssuedRefreshToken token = adapter.issueRefreshToken(createUser());
        RefreshTokenClaims claims = adapter.verifyRefreshToken(token.value());

        assertThat(claims.userId()).isEqualTo(1L);
        assertThat(claims.jti()).isNotBlank();
        assertThat(claims.expiresAt()).isEqualTo(NOW.plusSeconds(120));
    }

    @Test
    void rejectsExpiredRefreshToken() {
        IssuedRefreshToken token = createAdapter(NOW).issueRefreshToken(createUser());
        JwtTokenAdapter expiredTokenAdapter = createAdapter(NOW.plusSeconds(121));

        assertThatThrownBy(() -> expiredTokenAdapter.verifyRefreshToken(token.value()))
                .isInstanceOf(AuthDomainException.class);
    }

    @Test
    void rejectsAccessTokenAsRefreshToken() {
        JwtTokenAdapter adapter = createAdapter(NOW);
        String accessToken = adapter.issueAccessToken(createUser()).value();

        assertThatThrownBy(() -> adapter.verifyRefreshToken(accessToken))
                .isInstanceOf(AuthDomainException.class);
    }

    @Test
    void rejectsTamperedRefreshToken() {
        JwtTokenAdapter adapter = createAdapter(NOW);
        String token = adapter.issueRefreshToken(createUser()).value();
        int signatureStart = token.lastIndexOf('.') + 1;
        char replacement = token.charAt(signatureStart) == 'A' ? 'B' : 'A';
        String tamperedToken = token.substring(0, signatureStart)
                + replacement
                + token.substring(signatureStart + 1);

        assertThatThrownBy(() -> adapter.verifyRefreshToken(tamperedToken))
                .isInstanceOf(AuthDomainException.class);
    }

    private JwtTokenAdapter createAdapter(Instant instant) {
        JwtProperties properties = new JwtProperties(ACCESS_SECRET, REFRESH_SECRET, 60L, 120L);
        return new JwtTokenAdapter(properties, Clock.fixed(instant, ZoneOffset.UTC));
    }

    private AuthenticatedUserInfo createUser() {
        return new AuthenticatedUserInfo(
                1L,
                "gildong@example.com",
                "USER"
        );
    }
}
