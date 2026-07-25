package com.promsearch.auth.application.service.command;

import com.promsearch.auth.application.port.out.oauth.SocialLoginPort;
import com.promsearch.auth.application.port.out.oauth.SocialLoginResult;
import com.promsearch.auth.application.usecase.SocialLoginUseCase;
import com.promsearch.auth.application.usecase.dto.LoginInfo;
import com.promsearch.auth.application.usecase.dto.SocialLoginCommand;
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
public class SocialAuthenticationService implements SocialLoginUseCase {

    private final List<SocialLoginPort> socialLoginPorts;
    private final SocialLoginCompletionService socialLoginCompletionService;

    @Override
    public LoginInfo socialLogin(SocialLoginCommand command) {
        SocialLoginPort port = resolvePort(command.provider());
        SocialLoginResult socialLoginResult = port.exchangeCodeAndFetchUserInfo(
                command.authorizationCode(), command.redirectUri());

        LoginInfo loginInfo = socialLoginCompletionService.completeLogin(command.provider(), socialLoginResult);

        log.info("auth_social_login_succeeded userId={} provider={}", loginInfo.userId(), command.provider());

        return loginInfo;
    }

    private SocialLoginPort resolvePort(SocialProvider provider) {
        return socialLoginPorts.stream()
                .filter(port -> port.provider() == provider)
                .findFirst()
                .orElseThrow(() -> new AuthDomainException(AuthErrorCode.UNSUPPORTED_OAUTH_PROVIDER));
    }
}
