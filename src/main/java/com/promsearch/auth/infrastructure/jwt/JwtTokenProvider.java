package com.promsearch.auth.infrastructure.jwt;

import com.promsearch.auth.application.usecase.dto.AuthenticatedUserInfo;
import com.promsearch.auth.application.port.out.token.AccessTokenProvider;
import com.promsearch.auth.application.port.out.token.AccessTokenProvider.AccessTokenClaims;
import com.promsearch.auth.application.port.out.token.RefreshTokenProvider;
import com.promsearch.auth.domain.exception.AuthDomainException;
import com.promsearch.auth.domain.exception.AuthErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider implements AccessTokenProvider, RefreshTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    private static final int MIN_HMAC_KEY_BYTES = 32;

    private final JwtProperties jwtProperties;
    private final Clock clock;
    private final SecretKey accessSecretKey;
    private final SecretKey refreshSecretKey;
    private final JwtParser accessJwtParser;
    private final JwtParser refreshJwtParser;

    @Autowired
    public JwtTokenProvider(JwtProperties jwtProperties) {
        this(jwtProperties, Clock.systemUTC());
    }

    JwtTokenProvider(JwtProperties jwtProperties, Clock clock) {
        this.jwtProperties = jwtProperties;
        this.clock = clock;
        this.accessSecretKey = createSecretKey("auth.jwt.access-secret", jwtProperties.accessSecret());
        this.refreshSecretKey = createSecretKey("auth.jwt.refresh-secret", jwtProperties.refreshSecret());
        this.accessJwtParser = Jwts.parser()
                .verifyWith(accessSecretKey)
                .clock(() -> Date.from(Instant.now(clock)))
                .build();
        this.refreshJwtParser = Jwts.parser()
                .verifyWith(refreshSecretKey)
                .clock(() -> Date.from(Instant.now(clock)))
                .build();
    }

    @Override
    public String createAccessToken(AuthenticatedUserInfo user) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plusSeconds(getAccessTokenExpirationSeconds());

        return Jwts.builder()
                .subject(String.valueOf(user.userId()))
                .claim("userId", user.userId())
                .claim("role", user.role())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(accessSecretKey, Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public AccessTokenClaims parseAccessToken(String accessToken) {
        try {
            Claims claims = accessJwtParser.parseSignedClaims(accessToken).getPayload();
            return new AccessTokenClaims(getUserId(claims), getRole(claims));
        } catch (ExpiredJwtException e) {
            log.warn("Expired access token rejected.");
            throw new AuthDomainException(AuthErrorCode.ACCESS_TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid access token rejected. reason={}", e.getClass().getSimpleName());
            throw new AuthDomainException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    @Override
    public Long getAccessTokenExpirationSeconds() {
        return jwtProperties.accessTokenExpirationSeconds();
    }

    @Override
    public RefreshToken createRefreshToken(AuthenticatedUserInfo user) {
        Instant now = Instant.ofEpochSecond(Instant.now(clock).getEpochSecond());
        Instant expiresAt = now.plusSeconds(jwtProperties.refreshTokenExpirationSeconds());

        String token = Jwts.builder()
                .subject(String.valueOf(user.userId()))
                .claim("userId", user.userId())
                .claim("tokenType", REFRESH_TOKEN_TYPE)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(refreshSecretKey, Jwts.SIG.HS256)
                .compact();

        return new RefreshToken(token, expiresAt);
    }

    @Override
    public RefreshTokenClaims parse(String refreshToken) {
        try {
            Claims claims = refreshJwtParser.parseSignedClaims(refreshToken).getPayload();
            validateRefreshTokenClaims(claims);
            return new RefreshTokenClaims(
                    getUserId(claims),
                    claims.getId(),
                    claims.getExpiration().toInstant()
            );
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid refresh token rejected. reason={}", e.getClass().getSimpleName());
            throw new AuthDomainException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    private void validateRefreshTokenClaims(Claims claims) {
        if (!REFRESH_TOKEN_TYPE.equals(claims.get("tokenType", String.class))
                || claims.getId() == null
                || claims.getId().isBlank()
                || claims.getExpiration() == null) {
            throw new AuthDomainException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    private Long getUserId(Claims claims) {
        Object userId = claims.get("userId");
        if (userId instanceof Number number) {
            return number.longValue();
        }
        if (userId instanceof String value) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                throw new AuthDomainException(AuthErrorCode.INVALID_TOKEN);
            }
        }
        throw new AuthDomainException(AuthErrorCode.INVALID_TOKEN);
    }

    private String getRole(Claims claims) {
        String role = claims.get("role", String.class);
        if (role == null || role.isBlank()) {
            throw new AuthDomainException(AuthErrorCode.INVALID_TOKEN);
        }
        return role;
    }

    private SecretKey createSecretKey(String propertyName, String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(propertyName + " is required.");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_HMAC_KEY_BYTES) {
            throw new IllegalStateException(propertyName + " must be at least 32 bytes for HS256.");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
