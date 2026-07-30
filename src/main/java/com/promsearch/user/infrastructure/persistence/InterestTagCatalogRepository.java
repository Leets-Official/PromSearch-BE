package com.promsearch.user.infrastructure.persistence;

import com.promsearch.prompt.domain.enums.TagType;
import com.promsearch.prompt.infrastructure.persistence.entity.TagJpaEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterestTagCatalogRepository extends JpaRepository<TagJpaEntity, Long> {

    List<TagJpaEntity> findAllByTagTypeAndTagNameIn(TagType tagType, Collection<String> tagNames);

    boolean existsByTagTypeAndTagName(TagType tagType, String tagName);
}
