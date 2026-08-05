package com.promsearch.user.infrastructure.persistence;

import com.promsearch.user.infrastructure.persistence.entity.UserAgreementJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAgreementRepository extends JpaRepository<UserAgreementJpaEntity, Long> {
}
