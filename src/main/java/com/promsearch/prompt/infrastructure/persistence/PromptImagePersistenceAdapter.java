package com.promsearch.prompt.infrastructure.persistence;

import com.promsearch.prompt.application.port.out.promptimage.LoadPromptImagePort;
import com.promsearch.prompt.application.port.out.promptimage.SavePromptImagePort;
import com.promsearch.prompt.domain.PromptImage;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import com.promsearch.prompt.infrastructure.persistence.entity.PromptImageJpaEntity;
import java.util.List;
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

    private PromptImageJpaEntity getEntity(UUID imageId) {
        return promptImageRepository.findById(imageId)
                .orElseThrow(() -> new PromptDomainException(PromptErrorCode.IMAGE_NOT_FOUND));
    }
}
