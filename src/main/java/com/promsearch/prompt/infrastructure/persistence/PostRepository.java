package com.promsearch.prompt.infrastructure.persistence;

import com.promsearch.prompt.infrastructure.persistence.entity.PostJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<PostJpaEntity, Long> {
}
