package com.promsearch.auth.infrastructure.persistence;

import com.promsearch.auth.application.port.out.SocialAccountRepository;
import com.promsearch.auth.domain.SocialAccount;
import com.promsearch.auth.domain.enums.SocialProvider;
import com.promsearch.auth.domain.exception.AuthDomainException;
import com.promsearch.auth.domain.exception.AuthErrorCode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SocialAccountPersistenceAdapter implements SocialAccountRepository {

    private final SocialAccountJpaRepository socialAccountJpaRepository;

    @Override
    public SocialAccount save(SocialAccount socialAccount) {
        try {
            return socialAccountJpaRepository.saveAndFlush(SocialAccountJpaEntity.from(socialAccount)).toDomain();
        } catch (DataIntegrityViolationException e) {
            throw new AuthDomainException(AuthErrorCode.SOCIAL_ACCOUNT_ALREADY_LINKED);
        }
    }

    @Override
    public Optional<SocialAccount> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId) {
        return socialAccountJpaRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .map(SocialAccountJpaEntity::toDomain);
    }
}
