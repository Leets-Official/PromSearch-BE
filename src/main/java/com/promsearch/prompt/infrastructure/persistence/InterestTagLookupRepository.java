package com.promsearch.prompt.infrastructure.persistence;

import com.promsearch.prompt.domain.enums.TagType;
import com.promsearch.prompt.infrastructure.persistence.entity.TagJpaEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InterestTagLookupRepository extends JpaRepository<TagJpaEntity, Long> {

    @Query("select tag.id from TagJpaEntity tag "
            + "where tag.tagType = :type and tag.tagName in :names")
    List<Long> findIdsByTypeAndNames(
            @Param("type") TagType type,
            @Param("names") Collection<String> names
    );
}
