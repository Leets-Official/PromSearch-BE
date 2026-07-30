package com.promsearch.community.infrastructure.persistence;

import com.promsearch.community.domain.enums.InteractionType;
import com.promsearch.community.infrastructure.persistence.entity.PostInteractionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostInteractionRepository extends JpaRepository<PostInteractionJpaEntity, Long> {

    boolean existsByUserIdAndPostIdAndInteractionType(
            Long userId,
            Long postId,
            InteractionType interactionType
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from PostInteractionJpaEntity interaction
            where interaction.userId = :userId
              and interaction.postId = :postId
              and interaction.interactionType = :interactionType
            """)
    int deleteByUserIdAndPostIdAndInteractionType(
            @Param("userId") Long userId,
            @Param("postId") Long postId,
            @Param("interactionType") InteractionType interactionType
    );
}
