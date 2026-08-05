package com.promsearch.prompt.infrastructure.persistence;

import com.promsearch.prompt.infrastructure.persistence.entity.TagJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserInterestTagLookupRepository extends JpaRepository<TagJpaEntity, Long> {

    @Query("select t.id as tagId, t.tagType as type, t.tagName as tagName "
            + "from UserInterestTagJpaEntity uit "
            + "join TagJpaEntity t on t.id = uit.id.tagId "
            + "where uit.id.userId = :userId "
            + "order by t.id asc")
    List<UserInterestTagProjection> findTagsByUserId(@Param("userId") Long userId);
}
