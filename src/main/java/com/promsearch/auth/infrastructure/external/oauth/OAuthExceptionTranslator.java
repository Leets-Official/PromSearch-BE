package com.promsearch.auth.infrastructure.external.oauth;

import com.promsearch.auth.domain.enums.SocialProvider;
import com.promsearch.auth.domain.exception.AuthDomainException;
import com.promsearch.auth.domain.exception.AuthErrorCode;
import java.util.Set;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

@Slf4j
final class OAuthExceptionTranslator {

    private static final Set<Integer> AUTHENTICATION_FAILURE_STATUS_CODES = Set.of(400, 401, 403);

    private OAuthExceptionTranslator() {
    }

    static <T> T execute(SocialProvider provider, Supplier<T> call) {
        try {
            return call.get();
        } catch (AuthDomainException e) {
            throw e;
        } catch (HttpClientErrorException e) {
            AuthErrorCode errorCode = resolveClientErrorCode(e);
            log.warn("oauth_provider_client_error provider={} status={} errorCode={}",
                    provider, e.getStatusCode(), errorCode);
            throw new AuthDomainException(errorCode);
        } catch (HttpServerErrorException | ResourceAccessException e) {
            log.warn("oauth_provider_unavailable provider={} reason={}", provider, e.getClass().getSimpleName());
            throw new AuthDomainException(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE);
        } catch (RestClientException e) {
            log.warn("oauth_provider_bad_response provider={} reason={}", provider, e.getClass().getSimpleName());
            throw new AuthDomainException(AuthErrorCode.OAUTH_PROVIDER_BAD_RESPONSE);
        }
    }

    private static AuthErrorCode resolveClientErrorCode(HttpClientErrorException e) {
        int status = e.getStatusCode().value();
        if (status == 429) {
            return AuthErrorCode.OAUTH_PROVIDER_RATE_LIMITED;
        }
        if (AUTHENTICATION_FAILURE_STATUS_CODES.contains(status)) {
            return AuthErrorCode.OAUTH_AUTHENTICATION_FAILED;
        }
        return AuthErrorCode.OAUTH_PROVIDER_BAD_RESPONSE;
    }
}
