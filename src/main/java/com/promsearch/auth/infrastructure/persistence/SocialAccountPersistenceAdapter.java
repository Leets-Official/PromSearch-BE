package com.promsearch.auth.infrastructure.persistence;

import com.promsearch.auth.infrastructure.persistence.entity.SocialAccountJpaEntity;
import com.promsearch.auth.application.port.out.persistence.social.LoadSocialAccountPort;
import com.promsearch.auth.application.port.out.persistence.social.SaveSocialAccountPort;
import com.promsearch.auth.domain.SocialAccount;
import com.promsearch.auth.domain.enums.SocialProvider;
import com.promsearch.auth.domain.exception.AuthDomainException;
import com.promsearch.auth.domain.exception.AuthErrorCode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SocialAccountPersistenceAdapter implements LoadSocialAccountPort, SaveSocialAccountPort {

    private final SocialAccountRepository socialAccountRepository;

    @Override
    public SocialAccount save(SocialAccount socialAccount) {
        try {
            return socialAccountRepository.saveAndFlush(SocialAccountJpaEntity.from(socialAccount)).toDomain();
        } catch (DataIntegrityViolationException e) {
            if (socialAccountRepository.existsByProviderAndProviderUserId(
                    socialAccount.getProvider(), socialAccount.getProviderUserId())) {
                throw new AuthDomainException(AuthErrorCode.SOCIAL_ACCOUNT_ALREADY_LINKED);
            }
            throw e;
        }
    }

    @Override
    public Optional<SocialAccount> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId) {
        return socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .map(SocialAccountJpaEntity::toDomain);
    }
}
