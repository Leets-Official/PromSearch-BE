package com.promsearch.user.infrastructure.persistence;

import com.promsearch.user.infrastructure.persistence.entity.UserInterestTagJpaEntity;
import com.promsearch.user.infrastructure.persistence.entity.UserInterestTagId;
import com.promsearch.prompt.infrastructure.persistence.entity.TagJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserInterestTagRepository extends JpaRepository<UserInterestTagJpaEntity, UserInterestTagId> {

    void deleteAllByIdUserId(Long userId);

    @Query("select tag from UserInterestTagJpaEntity interest join TagJpaEntity tag on interest.id.tagId = tag.id "
            + "where interest.id.userId = :userId order by tag.id")
    List<TagJpaEntity> findInterestTagsByUserId(@Param("userId") Long userId);
}
