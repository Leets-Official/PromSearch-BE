package com.promsearch.user.infrastructure.persistence;

import com.promsearch.user.infrastructure.persistence.entity.UserInterestTagJpaEntity;
import com.promsearch.user.infrastructure.persistence.entity.UserInterestTagId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserInterestTagRepository extends JpaRepository<UserInterestTagJpaEntity, UserInterestTagId> {
}
