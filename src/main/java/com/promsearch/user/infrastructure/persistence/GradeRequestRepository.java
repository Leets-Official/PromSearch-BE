package com.promsearch.user.infrastructure.persistence;

import com.promsearch.user.domain.enums.GradeRequestStatus;
import com.promsearch.user.infrastructure.persistence.entity.GradeRequestJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GradeRequestRepository extends JpaRepository<GradeRequestJpaEntity, Long> {

    boolean existsByUserIdAndStatus(Long userId, GradeRequestStatus status);
}
