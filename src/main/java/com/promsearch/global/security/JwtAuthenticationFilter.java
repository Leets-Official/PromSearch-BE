package com.promsearch.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promsearch.auth.application.port.out.token.AccessTokenProvider;
import com.promsearch.auth.application.port.out.token.AccessTokenProvider.AccessTokenClaims;
import com.promsearch.auth.domain.exception.AuthDomainException;
import com.promsearch.auth.domain.exception.AuthErrorCode;
import com.promsearch.global.config.logging.RequestLoggingFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AccessTokenProvider accessTokenProvider;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization == null || authorization.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!authorization.startsWith(BEARER_PREFIX)) {
            ApiSecurityResponseWriter.write(response, objectMapper, AuthErrorCode.INVALID_TOKEN);
            return;
        }

        String accessToken = authorization.substring(BEARER_PREFIX.length()).trim();
        if (accessToken.isBlank()) {
            ApiSecurityResponseWriter.write(response, objectMapper, AuthErrorCode.INVALID_TOKEN);
            return;
        }

        try {
            AccessTokenClaims claims = accessTokenProvider.parseAccessToken(accessToken);
            AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(claims.userId(), claims.role());
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + claims.role()))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            RequestLoggingFilter.putAuthenticatedUserContext(request, principal.userId(), principal.role());
            filterChain.doFilter(request, response);
        } catch (AuthDomainException e) {
            SecurityContextHolder.clearContext();
            ApiSecurityResponseWriter.write(response, objectMapper, e.getBaseCode());
        }
    }
}
