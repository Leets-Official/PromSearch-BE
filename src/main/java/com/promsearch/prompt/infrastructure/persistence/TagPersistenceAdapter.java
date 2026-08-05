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
        List<TagJpaEntity> tags = tagType == null
                ? tagRepository.findAllByOrderByIdAsc()
                : tagRepository.findAllByTagTypeOrderByIdAsc(tagType);

        return tags.stream()
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
