package com.promsearch.prompt.infrastructure.persistence;

import com.promsearch.prompt.infrastructure.persistence.entity.PromptImageWatermarkOutboxJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptImageWatermarkOutboxRepository
        extends JpaRepository<PromptImageWatermarkOutboxJpaEntity, UUID> {
}
