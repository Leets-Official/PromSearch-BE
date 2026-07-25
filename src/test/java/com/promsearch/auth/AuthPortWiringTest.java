package com.promsearch.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.promsearch.auth.application.port.out.crypto.HashTokenPort;
import com.promsearch.auth.application.port.out.oauth.SocialLoginPort;
import com.promsearch.auth.application.port.out.token.IssueAccessTokenPort;
import com.promsearch.auth.application.port.out.token.IssueRefreshTokenPort;
import com.promsearch.auth.application.port.out.token.VerifyAccessTokenPort;
import com.promsearch.auth.application.port.out.token.VerifyRefreshTokenPort;
import com.promsearch.auth.application.service.query.AccessTokenAuthenticationService;
import com.promsearch.auth.application.usecase.AuthenticateAccessTokenUseCase;
import com.promsearch.auth.domain.enums.SocialProvider;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
class AuthPortWiringTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private List<SocialLoginPort> socialLoginPorts;

    @Test
    void singletonTokenAndCryptoPortsHaveExactlyOneImplementation() {
        assertThat(applicationContext.getBeansOfType(IssueAccessTokenPort.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(VerifyAccessTokenPort.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(IssueRefreshTokenPort.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(VerifyRefreshTokenPort.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(HashTokenPort.class)).hasSize(1);
    }

    @Test
    void accessTokenAuthenticationUseCaseHasOneQueryServiceImplementation() {
        assertThat(applicationContext.getBeansOfType(AuthenticateAccessTokenUseCase.class))
                .hasSize(1)
                .allSatisfy((name, bean) -> assertThat(bean)
                        .isInstanceOf(AccessTokenAuthenticationService.class));
    }

    @Test
    void everySupportedSocialProviderHasOneUniqueAdapter() {
        assertThat(socialLoginPorts)
                .extracting(SocialLoginPort::provider)
                .containsExactlyInAnyOrder(SocialProvider.KAKAO, SocialProvider.GOOGLE)
                .doesNotHaveDuplicates();
    }
}
