package com.promsearch.prompt.infrastructure.persistence;

import com.promsearch.prompt.domain.enums.PromptImageStatus;
import com.promsearch.prompt.infrastructure.persistence.entity.PromptImageJpaEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PromptImageRepository extends JpaRepository<PromptImageJpaEntity, UUID> {

    Optional<PromptImageJpaEntity> findByIdAndUploaderId(UUID id, Long uploaderId);

    /*
     * 프롬프트 생성 시 전달된 imageId(최대 10개)만 한 번에 검증한다.
     * 사용자 전체 이미지를 무제한 조회하는 메서드는 두지 않는다.
     */
    List<PromptImageJpaEntity> findAllByIdInAndUploaderIdAndStatus(
            Collection<UUID> ids,
            Long uploaderId,
            PromptImageStatus status
    );

    List<PromptImageJpaEntity> findAllByPromptIdOrderBySortOrderAsc(Long promptId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select image from PromptImageJpaEntity image where image.id in :ids order by image.id")
    List<PromptImageJpaEntity> findAllByIdInForUpdate(@Param("ids") Collection<UUID> ids);
}
