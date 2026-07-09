package com.promsearch.user.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {

    boolean existsByNickname(String nickname);

    boolean existsByEmail(String email);

    Optional<UserJpaEntity> findByEmail(String email);
}
