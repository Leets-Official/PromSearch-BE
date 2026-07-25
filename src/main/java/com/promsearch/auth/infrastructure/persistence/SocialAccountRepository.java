package com.promsearch.auth.infrastructure.persistence;

import com.promsearch.auth.infrastructure.persistence.entity.SocialAccountJpaEntity;
import com.promsearch.auth.domain.enums.SocialProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialAccountRepository extends JpaRepository<SocialAccountJpaEntity, Long> {

    Optional<SocialAccountJpaEntity> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);

    boolean existsByProviderAndProviderUserId(SocialProvider provider, String providerUserId);
}
