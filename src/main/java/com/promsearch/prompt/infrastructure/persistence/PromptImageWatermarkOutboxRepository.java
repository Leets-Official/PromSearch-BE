package com.promsearch.prompt.infrastructure.persistence;

import com.promsearch.prompt.infrastructure.persistence.entity.PromptImageWatermarkOutboxJpaEntity;
import com.promsearch.prompt.infrastructure.persistence.entity.PromptImageWatermarkOutboxJpaEntity.Status;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 워터마크 Outbox 영속화와 발행 대상 잠금 조회 */
public interface PromptImageWatermarkOutboxRepository
        extends JpaRepository<PromptImageWatermarkOutboxJpaEntity, UUID> {

    /** 생성 순서대로 발행 가능한 작업을 비관적 잠금으로 조회 */
    // TODO: API 발행 인스턴스가 늘어 잠금 대기가 확인되면 PostgreSQL SKIP LOCKED 기반
    // native query로 전환해 서로 다른 Outbox 배치를 병렬 선점
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select outbox
            from PromptImageWatermarkOutboxJpaEntity outbox
            where outbox.status = :status
              and outbox.availableAt <= :now
            order by outbox.createdAt asc
            """)
    List<PromptImageWatermarkOutboxJpaEntity> findPublishableForUpdate(
            @Param("status") Status status,
            @Param("now") Instant now,
            Pageable pageable
    );
}
