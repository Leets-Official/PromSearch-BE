package com.promsearch.prompt.infrastructure.persistence;

import com.promsearch.prompt.domain.enums.TagType;
import com.promsearch.prompt.infrastructure.persistence.entity.TagJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<TagJpaEntity, Long> {

    Optional<TagJpaEntity> findByTagTypeAndNormalizedName(TagType tagType, String normalizedName);

    List<TagJpaEntity> findAllByOrderByIdAsc();

    List<TagJpaEntity> findAllByTagTypeOrderByIdAsc(TagType tagType);
}
