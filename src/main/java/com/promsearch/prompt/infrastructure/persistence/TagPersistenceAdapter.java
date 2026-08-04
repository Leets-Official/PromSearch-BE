package com.promsearch.prompt.infrastructure.persistence;

import com.promsearch.prompt.application.port.out.tag.LoadTagPort;
import com.promsearch.prompt.application.port.out.tag.SaveTagPort;
import com.promsearch.prompt.domain.Tag;
import com.promsearch.prompt.domain.enums.TagType;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import com.promsearch.prompt.infrastructure.persistence.entity.TagJpaEntity;
import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TagPersistenceAdapter implements LoadTagPort, SaveTagPort {

    private final TagRepository tagRepository;
    private final EntityManager entityManager;

    @Override
    public List<Tag> batchGetByIds(Collection<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> uniqueTagIds = new LinkedHashSet<>(tagIds);
        List<TagJpaEntity> tags = tagRepository.findAllById(uniqueTagIds);
        if (tags.size() != uniqueTagIds.size()) {
            throw new PromptDomainException(PromptErrorCode.TAG_NOT_FOUND);
        }
        return tags.stream()
                .map(TagJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Tag> listByType(TagType tagType) {
        if (tagType == null) {
            return List.of();
        }

        /*
         * 필터 드롭다운은 고정된 순서로 보여야 사용자가 매번 같은 위치에서 선택할 수 있습니다.
         * 별도 displayOrder 컬럼이 생기기 전까지는 초기 시드/생성 순서에 가까운 ID 오름차순으로 반환합니다.
         */
        return tagRepository.findAllByTagTypeOrderByIdAsc(tagType).stream()
                .map(TagJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Tag create(Tag tag) {
        try {
            return tagRepository.saveAndFlush(TagJpaEntity.from(tag)).toDomain();
        } catch (DataIntegrityViolationException exception) {
            throw new PromptDomainException(
                    PromptErrorCode.DUPLICATE_TAG,
                    PromptErrorCode.DUPLICATE_TAG.getMessage(),
                    exception
            );
        }
    }

    @Override
    public Tag getOrCreateCustomAiModel(Tag tag) {
        if (tag == null || tag.getNormalizedName() == null || tag.getNormalizedName().isBlank()) {
            throw new PromptDomainException(PromptErrorCode.INVALID_TAG_NAME);
        }
        if (tag.getTagType() != TagType.AI_MODEL) {
            throw new PromptDomainException(PromptErrorCode.INVALID_TAG_TYPE);
        }

        String lockKey = TagType.AI_MODEL.name() + ":" + tag.getNormalizedName();
        entityManager.createNativeQuery(
                        "select pg_advisory_xact_lock(hashtextextended(cast(:lockKey as text), 0))"
                )
                .setParameter("lockKey", lockKey)
                .getSingleResult();

        return tagRepository.findByTagTypeAndNormalizedName(TagType.AI_MODEL, tag.getNormalizedName())
                .map(TagJpaEntity::toDomain)
                .orElseGet(() -> create(tag));
    }
}
