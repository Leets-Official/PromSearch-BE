package com.promsearch.user.application.service.command;

import com.promsearch.user.application.port.out.tag.ResolveInterestTagIdsPort;
import com.promsearch.user.application.port.out.user.LoadUserPort;
import com.promsearch.user.application.port.out.user.SaveUserAgreementPort;
import com.promsearch.user.application.port.out.user.SaveUserInterestTagPort;
import com.promsearch.user.application.port.out.user.SaveUserPort;
import com.promsearch.user.application.port.out.profileimage.ProfileImageDeliveryPort;
import com.promsearch.user.application.port.out.profileimage.ScheduleProfileImageDeletionPort;
import com.promsearch.user.application.usecase.ChangePasswordUseCase;
import com.promsearch.user.application.usecase.DeleteUserUseCase;
import com.promsearch.user.application.usecase.RegisterSocialUserUseCase;
import com.promsearch.user.application.usecase.SignupUseCase;
import com.promsearch.user.application.usecase.UpdateUserProfileUseCase;
import com.promsearch.user.application.usecase.dto.ChangePasswordCommand;
import com.promsearch.user.application.usecase.dto.RegisterSocialUserCommand;
import com.promsearch.user.application.usecase.dto.SignupCommand;
import com.promsearch.user.application.usecase.dto.SignupInfo;
import com.promsearch.user.application.usecase.dto.UpdateUserProfileCommand;
import com.promsearch.user.application.usecase.dto.UserInfo;
import com.promsearch.user.domain.User;
import com.promsearch.user.domain.InterestTagSelectionPolicy;
import com.promsearch.user.domain.NicknamePolicy;
import com.promsearch.user.domain.enums.InterestTagType;
import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class UserCommandService implements
        SignupUseCase,
        UpdateUserProfileUseCase,
        ChangePasswordUseCase,
        DeleteUserUseCase,
        RegisterSocialUserUseCase {

    private static final String DEFAULT_SOCIAL_NICKNAME = "user";
    private static final String DEFAULT_SOCIAL_NAME = "소셜 사용자";
    private static final int SOCIAL_NICKNAME_BASE_MAX_LENGTH = 6;
    private static final int NICKNAME_RETRY_LIMIT = 5;

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final ResolveInterestTagIdsPort resolveInterestTagIdsPort;
    private final SaveUserInterestTagPort saveUserInterestTagPort;
    private final SaveUserAgreementPort saveUserAgreementPort;
    private final PasswordEncoder passwordEncoder;
    private final ProfileImageDeliveryPort profileImageDeliveryPort;
    private final ScheduleProfileImageDeletionPort profileImageDeletionPort;

    @Override
    public SignupInfo signup(SignupCommand command) {
        validateDuplicateEmail(command.email());
        validateDuplicateNickname(command.nickname());
        InterestTagSelectionPolicy.validate(command.jobTags(), command.taskTags());

        String encodedPassword = passwordEncoder.encode(command.password());
        User user = User.create(
                command.email(),
                encodedPassword,
                command.nickname(),
                null,
                normalizeOptional(command.profileImageUrl())
        );

        User savedUser = saveUserPort.create(user);
        saveUserAgreementPort.saveAll(savedUser.getUserId().id(), command.agreements().toUserAgreements(Instant.now()));
        List<Long> interestTagIds = new ArrayList<>();
        interestTagIds.addAll(resolveInterestTagIdsPort.resolve(InterestTagType.JOB, command.jobTags()));
        interestTagIds.addAll(resolveInterestTagIdsPort.resolve(InterestTagType.TASK, command.taskTags()));
        saveUserInterestTagPort.save(savedUser.getUserId().id(), interestTagIds);
        SignupInfo signupInfo = SignupInfo.from(savedUser);
        log.info("user_signup_completed userId={}", signupInfo.userId());
        return signupInfo;
    }

    @Override
    public UserInfo updateProfile(UpdateUserProfileCommand command) {
        User user = loadUserPort.getById(command.userId());

        String name = resolveRequiredProfileValue(command.name(), user.getName(), UserErrorCode.INVALID_NAME);
        String nickname = resolveRequiredProfileValue(
                command.nickname(),
                user.getNickname(),
                UserErrorCode.INVALID_NICKNAME
        );
        String email = resolveRequiredProfileValue(command.email(), user.getEmail(), UserErrorCode.INVALID_EMAIL);
        if (!nickname.equals(user.getNickname())) {
            NicknamePolicy.validate(nickname);
            validateDuplicateNickname(nickname);
        }
        if (!email.equals(user.getEmail())) {
            validateDuplicateEmail(email);
        }

        User updatedUser = user.updateProfile(
                email,
                nickname,
                name
        );
        User savedUser = saveUserPort.update(updatedUser);
        UserInfo userInfo = UserInfo.from(savedUser, resolveProfileImageUrl(savedUser));
        log.info("user_profile_updated userId={}", userInfo.userId());
        return userInfo;
    }

    @Override
    public void changePassword(ChangePasswordCommand command) {
        User user = loadUserPort.getById(command.userId());
        String currentPassword = resolveRequiredPassword(command.currentPassword());
        String newPassword = resolveRequiredPassword(command.newPassword());

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new UserDomainException(UserErrorCode.INVALID_PASSWORD);
        }

        User updatedUser = user.changePassword(passwordEncoder.encode(newPassword));
        saveUserPort.update(updatedUser);
        log.info("user_password_changed userId={}", command.userId());
    }

    @Override
    public void delete(Long userId) {
        User user = loadUserPort.getById(userId);
        saveUserPort.update(user.delete());
        profileImageDeletionPort.afterCommit(user.getProfileImageObjectKey());
        log.info("user_deleted userId={}", userId);
    }

    @Override
    public SignupInfo registerSocialUser(RegisterSocialUserCommand command) {
        validateDuplicateEmail(command.email());

        String nickname = resolveAvailableNickname(command.nickname());
        String name = resolveSocialName(command.name());
        String placeholderPassword = passwordEncoder.encode(UUID.randomUUID().toString());

        User user = User.create(command.email(), placeholderPassword, nickname, name, command.profileImageUrl());

        SignupInfo signupInfo = SignupInfo.from(saveUserPort.create(user));
        log.info("user_social_signup_completed userId={}", signupInfo.userId());
        return signupInfo;
    }

    private String resolveAvailableNickname(String nicknameHint) {
        String base = normalizeNicknameHint(nicknameHint);
        String candidate = base;
        int attempt = 0;
        while (loadUserPort.existsByNickname(candidate)) {
            attempt++;
            String suffix = attempt <= NICKNAME_RETRY_LIMIT
                    ? String.valueOf(ThreadLocalRandom.current().nextInt(1000, 10000))
                    : UUID.randomUUID().toString().substring(0, 8);
            int baseLength = Math.min(base.length(), NicknamePolicy.MAX_LENGTH - suffix.length());
            candidate = base.substring(0, baseLength) + suffix;
        }
        return candidate;
    }

    private String normalizeNicknameHint(String nicknameHint) {
        String trimmed = nicknameHint == null ? "" : nicknameHint.trim();
        if (trimmed.isBlank()) {
            trimmed = DEFAULT_SOCIAL_NICKNAME;
        }
        String sanitized = trimmed.replaceAll("[^가-힣A-Za-z0-9]", "");
        if (sanitized.isBlank()) {
            sanitized = DEFAULT_SOCIAL_NICKNAME;
        }
        return sanitized.length() > SOCIAL_NICKNAME_BASE_MAX_LENGTH
                ? sanitized.substring(0, SOCIAL_NICKNAME_BASE_MAX_LENGTH)
                : sanitized;
    }

    private String resolveSocialName(String name) {
        return (name == null || name.isBlank()) ? DEFAULT_SOCIAL_NAME : name.trim();
    }

    private void validateDuplicateNickname(String nickname) {
        if (loadUserPort.existsByNickname(nickname)) {
            throw new UserDomainException(UserErrorCode.DUPLICATE_NICKNAME);
        }
    }

    private void validateDuplicateEmail(String email) {
        if (loadUserPort.existsByEmail(email)) {
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

    private String resolveProfileImageUrl(User user) {
        return profileImageDeliveryPort.resolve(
                user.getProfileImageUrl(),
                user.getProfileImageObjectKey()
        );
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
