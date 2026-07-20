package com.promsearch.auth.application;

import com.promsearch.auth.application.port.out.SocialLoginClient;
import com.promsearch.auth.application.port.out.SocialLoginClient.SocialUserInfo;
import com.promsearch.auth.domain.enums.SocialProvider;
import com.promsearch.auth.domain.exception.AuthDomainException;
import com.promsearch.auth.domain.exception.AuthErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SocialAuthCommandService implements SocialLoginUseCase {

    private final List<SocialLoginClient> socialLoginClients;
    private final SocialLoginTransactionService socialLoginTransactionService;

    @Override
    public LoginInfo socialLogin(SocialLoginCommand command) {
        SocialLoginClient client = resolveClient(command.provider());
        SocialUserInfo socialUserInfo = client.exchangeCodeAndFetchUserInfo(
                command.authorizationCode(), command.redirectUri());

        LoginInfo loginInfo = socialLoginTransactionService.completeLogin(command.provider(), socialUserInfo);

        log.info("auth_social_login_succeeded userId={} provider={}", loginInfo.userId(), command.provider());

        return loginInfo;
    }

    private SocialLoginClient resolveClient(SocialProvider provider) {
        return socialLoginClients.stream()
                .filter(client -> client.provider() == provider)
                .findFirst()
                .orElseThrow(() -> new AuthDomainException(AuthErrorCode.UNSUPPORTED_OAUTH_PROVIDER));
    }
}
