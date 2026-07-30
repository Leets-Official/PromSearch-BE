package com.promsearch.user.infrastructure.persistence;

import com.promsearch.user.infrastructure.persistence.entity.UserJpaEntity;
import com.promsearch.user.application.port.out.user.LoadUserPort;
import com.promsearch.user.application.port.out.user.SaveUserPort;
import com.promsearch.user.domain.User;
import com.promsearch.user.domain.enums.UserStatus;
import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements LoadUserPort, SaveUserPort {

    private final UserRepository userRepository;

    @Override
    public User create(User user) {
        try {
            UserJpaEntity savedUser = userRepository.saveAndFlush(UserMapper.toJpaEntity(user));
            return UserMapper.toDomain(savedUser);
        } catch (DataIntegrityViolationException e) {
            if (userRepository.existsByEmail(user.getEmail())) {
                throw new UserDomainException(UserErrorCode.DUPLICATE_EMAIL);
            }
            if (userRepository.existsByNickname(user.getNickname())) {
                throw new UserDomainException(UserErrorCode.DUPLICATE_NICKNAME);
            }
            throw e;
        }
    }

    @Override
    public User getById(Long userId) {
        return UserMapper.toDomain(getActiveUserJpaEntity(userId));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId)
                .map(UserMapper::toDomain);
    }

    @Override
    public boolean existsByNickname(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public User update(User user) {
        try {
            UserJpaEntity userJpaEntity = getActiveUserJpaEntity(user.getUserId().id());
            userJpaEntity.updateFrom(user);
            userRepository.flush();
            return UserMapper.toDomain(userJpaEntity);
        } catch (DataIntegrityViolationException e) {
            if (userRepository.existsByEmailAndIdNot(user.getEmail(), user.getUserId().id())) {
                throw new UserDomainException(UserErrorCode.DUPLICATE_EMAIL);
            }
            if (userRepository.existsByNicknameAndIdNot(user.getNickname(), user.getUserId().id())) {
                throw new UserDomainException(UserErrorCode.DUPLICATE_NICKNAME);
            }
            throw e;
        }
    }

    private UserJpaEntity getActiveUserJpaEntity(Long userId) {
        return userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new UserDomainException(UserErrorCode.USER_NOT_FOUND));
    }
}
