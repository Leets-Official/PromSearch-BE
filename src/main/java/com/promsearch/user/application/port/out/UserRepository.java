package com.promsearch.user.application.port.out;

import com.promsearch.user.domain.User;

public interface UserRepository {

    User save(User user);

    boolean existsByNickname(String nickname);

    boolean existsByEmail(String email);
}
