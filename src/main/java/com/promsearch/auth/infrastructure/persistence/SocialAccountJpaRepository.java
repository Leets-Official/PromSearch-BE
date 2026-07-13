package com.promsearch.auth.infrastructure.persistence;

import com.promsearch.auth.domain.enums.SocialProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialAccountJpaRepository extends JpaRepository<SocialAccountJpaEntity, Long> {

    Optional<SocialAccountJpaEntity> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);
}
