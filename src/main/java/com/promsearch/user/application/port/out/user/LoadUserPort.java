package com.promsearch.user.application.port.out.user;

import com.promsearch.user.domain.User;
import java.util.Optional;

public interface LoadUserPort {

    User getById(Long userId);

    Optional<User> findByEmail(String email);

    Optional<User> findById(Long userId);

    boolean existsByNickname(String nickname);

    boolean existsByEmail(String email);
}
