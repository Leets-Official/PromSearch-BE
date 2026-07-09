package com.promsearch.auth.infrastructure.jwt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promsearch.auth.application.port.out.AccessTokenProvider;
import com.promsearch.user.domain.User;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider implements AccessTokenProvider {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String TOKEN_TYPE = "JWT";
    private static final String ALGORITHM = "HS256";

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

    private String base64UrlEncode(byte[] value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value);
    }
}
