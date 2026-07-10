package com.promsearch.auth.infrastructure.jwt;

import com.promsearch.auth.application.port.out.AccessTokenProvider;
import com.promsearch.auth.application.port.out.RefreshTokenProvider;
import com.promsearch.auth.domain.exception.AuthDomainException;
import com.promsearch.auth.domain.exception.AuthErrorCode;
import com.promsearch.user.application.AuthUserInfo;
import io.jsonwebtoken.Claims;
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
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider implements AccessTokenProvider, RefreshTokenProvider {

    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final JwtProperties jwtProperties;
    private final Clock clock;
    private final SecretKey secretKey;
    private final JwtParser jwtParser;

    @Autowired
    public JwtTokenProvider(JwtProperties jwtProperties) {
        this(jwtProperties, Clock.systemUTC());
    }

    JwtTokenProvider(JwtProperties jwtProperties, Clock clock) {
        this.jwtProperties = jwtProperties;
        this.clock = clock;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        this.jwtParser = Jwts.parser()
                .verifyWith(secretKey)
                .clock(() -> Date.from(Instant.now(clock)))
                .build();
    }

    @Override
    public String createAccessToken(AuthUserInfo user) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plusSeconds(getAccessTokenExpirationSeconds());

        return Jwts.builder()
                .subject(String.valueOf(user.userId()))
                .claim("userId", user.userId())
                .claim("email", user.email())
                .claim("role", user.role())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public Long getAccessTokenExpirationSeconds() {
        return jwtProperties.accessTokenExpirationSeconds();
    }

    @Override
    public RefreshToken createRefreshToken(AuthUserInfo user) {
        Instant now = Instant.ofEpochSecond(Instant.now(clock).getEpochSecond());
        Instant expiresAt = now.plusSeconds(jwtProperties.refreshTokenExpirationSeconds());

        String token = Jwts.builder()
                .subject(String.valueOf(user.userId()))
                .claim("userId", user.userId())
                .claim("tokenType", REFRESH_TOKEN_TYPE)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();

        return new RefreshToken(token, expiresAt);
    }

    @Override
    public RefreshTokenClaims parse(String refreshToken) {
        try {
            Claims claims = jwtParser.parseSignedClaims(refreshToken).getPayload();
            validateRefreshTokenClaims(claims);
            return new RefreshTokenClaims(
                    getUserId(claims),
                    claims.getId(),
                    claims.getExpiration().toInstant()
            );
        } catch (JwtException | IllegalArgumentException e) {
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
}
