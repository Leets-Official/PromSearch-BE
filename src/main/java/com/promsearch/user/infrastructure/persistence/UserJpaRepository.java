package com.promsearch.user.infrastructure.persistence;

import com.promsearch.user.domain.enums.UserStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {

    Optional<UserJpaEntity> findByIdAndStatus(Long id, UserStatus status);

    Optional<UserJpaEntity> findByEmail(String email);

    boolean existsByNickname(String nickname);

    boolean existsByEmail(String email);

    boolean existsByNicknameAndIdNot(String nickname, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);
}
