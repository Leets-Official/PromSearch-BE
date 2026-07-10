package com.promsearch.user.application;

import com.promsearch.user.application.port.out.UserRepository;
import com.promsearch.user.domain.User;
import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserCommandService implements SignupUseCase, UpdateUserProfileUseCase, ChangePasswordUseCase, DeleteUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public SignupInfo signup(SignupCommand command) {
        validateDuplicateEmail(command.email());
        validateDuplicateNickname(command.nickname());

        String encodedPassword = passwordEncoder.encode(command.password());
        User user = User.create(
                command.email(),
                encodedPassword,
                command.nickname(),
                command.name(),
                null
        );

        return SignupInfo.from(userRepository.create(user));
    }

    @Override
    public UserInfo updateProfile(UpdateUserProfileCommand command) {
        User user = userRepository.getById(command.userId());

        String name = resolveRequiredProfileValue(command.name(), user.getName(), UserErrorCode.INVALID_NAME);
        String nickname = resolveRequiredProfileValue(
                command.nickname(),
                user.getNickname(),
                UserErrorCode.INVALID_NICKNAME
        );
        String email = resolveRequiredProfileValue(command.email(), user.getEmail(), UserErrorCode.INVALID_EMAIL);
        String profileImageUrl = resolveOptionalProfileValue(command.profileImageUrl(), user.getProfileImageUrl());

        if (!nickname.equals(user.getNickname())) {
            validateDuplicateNickname(nickname);
        }
        if (!email.equals(user.getEmail())) {
            validateDuplicateEmail(email);
        }

        User updatedUser = user.updateProfile(
                email,
                nickname,
                name,
                profileImageUrl
        );
        return UserInfo.from(userRepository.update(updatedUser));
    }

    @Override
    public void changePassword(ChangePasswordCommand command) {
        User user = userRepository.getById(command.userId());
        String currentPassword = resolveRequiredPassword(command.currentPassword());
        String newPassword = resolveRequiredPassword(command.newPassword());

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new UserDomainException(UserErrorCode.INVALID_PASSWORD);
        }

        User updatedUser = user.changePassword(passwordEncoder.encode(newPassword));
        userRepository.update(updatedUser);
    }

    @Override
    public void delete(Long userId) {
        User user = userRepository.getById(userId);
        userRepository.update(user.delete());
    }

    private void validateDuplicateNickname(String nickname) {
        if (userRepository.existsByNickname(nickname)) {
            throw new UserDomainException(UserErrorCode.DUPLICATE_NICKNAME);
        }
    }

    private void validateDuplicateEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new UserDomainException(UserErrorCode.DUPLICATE_EMAIL);
        }
    }

    private String resolveRequiredProfileValue(String requestedValue, String currentValue, UserErrorCode errorCode) {
        if (requestedValue == null) {
            return currentValue;
        }
        if (requestedValue.isBlank()) {
            throw new UserDomainException(errorCode);
        }
        return requestedValue.trim();
    }

    private String resolveRequiredPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new UserDomainException(UserErrorCode.INVALID_PASSWORD);
        }
        return password;
    }

    private String resolveOptionalProfileValue(String requestedValue, String currentValue) {
        if (requestedValue == null) {
            return currentValue;
        }
        if (requestedValue.isBlank()) {
            return null;
        }
        return requestedValue.trim();
    }
}
