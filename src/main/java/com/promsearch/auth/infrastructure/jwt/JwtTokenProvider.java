package com.promsearch.auth.infrastructure.jwt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promsearch.auth.application.port.out.AccessTokenProvider;
import com.promsearch.auth.application.port.out.RefreshTokenProvider;
import com.promsearch.auth.domain.exception.AuthDomainException;
import com.promsearch.auth.domain.exception.AuthErrorCode;
import com.promsearch.user.domain.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider implements AccessTokenProvider, RefreshTokenProvider {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String TOKEN_TYPE = "JWT";
    private static final String ALGORITHM = "HS256";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final JwtProperties jwtProperties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public JwtTokenProvider(JwtProperties jwtProperties, ObjectMapper objectMapper) {
        this(jwtProperties, objectMapper, Clock.systemUTC());
    }

    JwtTokenProvider(JwtProperties jwtProperties, ObjectMapper objectMapper, Clock clock) {
        this.jwtProperties = jwtProperties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public String createAccessToken(User user) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plusSeconds(getAccessTokenExpirationSeconds());

        Map<String, Object> header = Map.of(
                "typ", TOKEN_TYPE,
                "alg", ALGORITHM
        );
        Map<String, Object> payload = Map.of(
                "sub", String.valueOf(user.getUserId().id()),
                "userId", user.getUserId().id(),
                "email", user.getEmail(),
                "role", user.getRole().name(),
                "iat", now.getEpochSecond(),
                "exp", expiresAt.getEpochSecond()
        );

        String unsignedToken = encodeJson(header) + "." + encodeJson(payload);
        return unsignedToken + "." + sign(unsignedToken);
    }

    @Override
    public Long getAccessTokenExpirationSeconds() {
        return jwtProperties.accessTokenExpirationSeconds();
    }

    @Override
    public String createRefreshToken(User user) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plusSeconds(jwtProperties.refreshTokenExpirationSeconds());

        Map<String, Object> header = Map.of(
                "typ", TOKEN_TYPE,
                "alg", ALGORITHM
        );
        Map<String, Object> payload = Map.of(
                "sub", String.valueOf(user.getUserId().id()),
                "userId", user.getUserId().id(),
                "tokenType", REFRESH_TOKEN_TYPE,
                "iat", now.getEpochSecond(),
                "exp", expiresAt.getEpochSecond()
        );

        String unsignedToken = encodeJson(header) + "." + encodeJson(payload);
        return unsignedToken + "." + sign(unsignedToken);
    }

    @Override
    public Long getUserId(String refreshToken) {
        Map<String, Object> payload = parseAndValidate(refreshToken);
        if (!REFRESH_TOKEN_TYPE.equals(payload.get("tokenType"))) {
            throw new AuthDomainException(AuthErrorCode.INVALID_TOKEN);
        }

        Object userId = payload.get("userId");
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

    private String encodeJson(Map<String, Object> value) {
        try {
            return base64UrlEncode(objectMapper.writeValueAsBytes(value));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize JWT content.", e);
        }
    }

    private String sign(String unsignedToken) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec keySpec = new SecretKeySpec(
                    jwtProperties.secret().getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256
            );
            mac.init(keySpec);
            return base64UrlEncode(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign JWT.", e);
        }
    }

    private Map<String, Object> parseAndValidate(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new AuthDomainException(AuthErrorCode.INVALID_TOKEN);
            }

            String unsignedToken = parts[0] + "." + parts[1];
            if (!MessageDigest.isEqual(sign(unsignedToken).getBytes(StandardCharsets.UTF_8),
                    parts[2].getBytes(StandardCharsets.UTF_8))) {
                throw new AuthDomainException(AuthErrorCode.INVALID_TOKEN);
            }

            Map<String, Object> payload = objectMapper.readValue(
                    base64UrlDecode(parts[1]),
                    new TypeReference<>() {
                    }
            );
            validateExpiration(payload);
            return payload;
        } catch (AuthDomainException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthDomainException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    private void validateExpiration(Map<String, Object> payload) {
        Object exp = payload.get("exp");
        long expiresAt;
        if (exp instanceof Number number) {
            expiresAt = number.longValue();
        } else if (exp instanceof String value) {
            expiresAt = Long.parseLong(value);
        } else {
            throw new AuthDomainException(AuthErrorCode.INVALID_TOKEN);
        }

        if (Instant.now(clock).getEpochSecond() >= expiresAt) {
            throw new AuthDomainException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    private String base64UrlEncode(byte[] value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value);
    }

    private byte[] base64UrlDecode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }
}
