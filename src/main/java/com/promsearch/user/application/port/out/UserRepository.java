package com.promsearch.user.application.port.out;

import com.promsearch.user.domain.User;
import java.util.Optional;

public interface UserRepository {

    User create(User user);

    Optional<User> findById(Long userId);

    Optional<User> findByEmail(String email);

    boolean existsByNickname(String nickname);

    boolean existsByEmail(String email);
}
