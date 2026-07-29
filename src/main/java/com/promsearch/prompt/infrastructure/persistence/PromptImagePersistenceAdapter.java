package com.promsearch.prompt.infrastructure.persistence;

import com.promsearch.prompt.application.port.out.promptimage.LoadPromptImagePort;
import com.promsearch.prompt.application.port.out.promptimage.SavePromptImagePort;
import com.promsearch.prompt.domain.PromptImage;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import com.promsearch.prompt.infrastructure.persistence.entity.PromptImageJpaEntity;
import java.util.Collection;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 이미지 도메인·JPA 엔티티 변환 및 영속화 */
@Component
@RequiredArgsConstructor
public class PromptImagePersistenceAdapter implements LoadPromptImagePort, SavePromptImagePort {

    private final PromptImageRepository promptImageRepository;

    /** JPA 엔티티 조회 및 도메인 객체 반환 */
    @Override
    public PromptImage getById(UUID imageId) {
        if (imageId == null) {
            throw new PromptDomainException(PromptErrorCode.INVALID_ID);
        }
        return getEntity(imageId).toDomain();
    }

    @Override
    public List<PromptImage> batchGetByIdsForUpdate(Collection<UUID> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) {
            return List.of();
        }

        List<PromptImageJpaEntity> entities = promptImageRepository.findAllByIdInForUpdate(imageIds);
        if (entities.size() != imageIds.size()) {
            throw new PromptDomainException(PromptErrorCode.IMAGE_NOT_FOUND);
        }
        return entities.stream()
                .map(PromptImageJpaEntity::toDomain)
                .toList();
    }

    /** 이미지 식별자 목록 기반 JPA 엔티티 일괄 조회 */
    @Override
    public List<PromptImage> listByIds(Collection<UUID> imageIds) {
        if (imageIds == null || imageIds.stream().anyMatch(java.util.Objects::isNull)) {
            throw new PromptDomainException(PromptErrorCode.INVALID_ID);
        }
        return promptImageRepository.findAllByIdIn(imageIds).stream()
                .map(PromptImageJpaEntity::toDomain)
                .toList();
    }

    /** 업로드 준비 이미지 일괄 저장 및 DB 제약 확인 */
    @Override
    public void createAll(List<PromptImage> images) {
        List<PromptImageJpaEntity> entities = images.stream()
                .map(PromptImageJpaEntity::from)
                .toList();
        promptImageRepository.saveAllAndFlush(entities);
    }

    /** 도메인 상태를 영속 엔티티에 반영 후 갱신 결과 반환 */
    @Override
    public PromptImage update(PromptImage image) {
        PromptImageJpaEntity entity = getEntity(image.getPromptImageId().id());
        entity.updateFrom(image);
        promptImageRepository.flush();
        return entity.toDomain();
    }

    @Override
    public void updateAll(List<PromptImage> images) {
        if (images == null || images.isEmpty()) {
            return;
        }

        Map<UUID, PromptImageJpaEntity> entitiesById = new HashMap<>();
        promptImageRepository.findAllById(images.stream()
                        .map(image -> image.getPromptImageId().id())
                        .toList())
                .forEach(entity -> entitiesById.put(entity.getId(), entity));
        if (entitiesById.size() != images.size()) {
            throw new PromptDomainException(PromptErrorCode.IMAGE_NOT_FOUND);
        }

        for (PromptImage image : images) {
            entitiesById.get(image.getPromptImageId().id()).updateFrom(image);
        }
        promptImageRepository.flush();
    }

    /** 필수 이미지 엔티티를 조회하고 누락 시 prompt 도메인 오류로 변환 */
    private PromptImageJpaEntity getEntity(UUID imageId) {
        return promptImageRepository.findById(imageId)
                .orElseThrow(() -> new PromptDomainException(PromptErrorCode.IMAGE_NOT_FOUND));
    }
}
