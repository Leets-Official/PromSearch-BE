package com.promsearch.user.application.port.out;

import com.promsearch.user.domain.User;

public interface UserRepository {

    User create(User user);

    User getById(Long userId);

    boolean existsByNickname(String nickname);

    boolean existsByEmail(String email);

    User updateProfile(
            Long userId,
            String email,
            String password,
            String nickname,
            String name,
            String profileImageUrl
    );

    void deleteById(Long userId);
}
