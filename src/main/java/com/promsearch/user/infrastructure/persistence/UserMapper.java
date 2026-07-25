package com.promsearch.user.infrastructure.persistence;

import com.promsearch.user.infrastructure.persistence.entity.UserJpaEntity;
import com.promsearch.user.domain.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserJpaEntity toJpaEntity(User user) {
        return UserJpaEntity.create(
                user.getEmail(),
                user.getPassword(),
                user.getNickname(),
                user.getName(),
                user.getProfileImageUrl()
        );
    }

    public static User toDomain(UserJpaEntity userJpaEntity) {
        return userJpaEntity.toDomain();
    }
}
