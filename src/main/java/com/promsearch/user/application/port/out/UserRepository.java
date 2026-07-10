package com.promsearch.user.application.port.out;

import com.promsearch.user.domain.User;

public interface UserRepository {

    User create(User user);

    User getById(Long userId);

    boolean existsByNickname(String nickname);

    boolean existsByEmail(String email);

    User update(User user);
}
