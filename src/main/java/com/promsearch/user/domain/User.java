package com.promsearch.user.domain;

import com.promsearch.user.domain.enums.UserGrade;
import com.promsearch.user.domain.enums.UserRole;
import com.promsearch.user.domain.enums.UserStatus;
import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class User {

    private final UserId userId;
    private final String email;
    private final String password;
    private final String nickname;
    private final String name;
    private final String profileImageUrl;
    private final Long point;
    private final UserRole role;
    private final UserGrade grade;
    private final UserStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private User(
            UserId userId,
            String email,
            String password,
            String nickname,
            String name,
            String profileImageUrl,
            Long point,
            UserRole role,
            UserGrade grade,
            UserStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.point = point;
        this.role = role;
        this.grade = grade;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static User create(String email, String password, String nickname, String name, String profileImageUrl) {
        validateRequired(email, password, nickname, name, 0L, UserRole.USER, UserGrade.NORMAL, UserStatus.ACTIVE);

        Instant now = Instant.now();
        return User.builder()
                .email(email)
                .password(password)
                .nickname(nickname)
                .name(name)
                .profileImageUrl(profileImageUrl)
                .point(0L)
                .role(UserRole.USER)
                .grade(UserGrade.NORMAL)
                .status(UserStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public static User reconstruct(
            UserId userId,
            String email,
            String password,
            String nickname,
            String name,
            String profileImageUrl,
            Long point,
            UserRole role,
            UserGrade grade,
            UserStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        validateRequired(email, password, nickname, name, point, role, grade, status);

        return User.builder()
                .userId(userId)
                .email(email)
                .password(password)
                .nickname(nickname)
                .name(name)
                .profileImageUrl(profileImageUrl)
                .point(point)
                .role(role)
                .grade(grade)
                .status(status)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    public User updateProfile(String email, String password, String nickname, String name, String profileImageUrl) {
        validateRequired(email, password, nickname, name, point, role, grade, status);

        return User.builder()
                .userId(userId)
                .email(email)
                .password(password)
                .nickname(nickname)
                .name(name)
                .profileImageUrl(profileImageUrl)
                .point(point)
                .role(role)
                .grade(grade)
                .status(status)
                .createdAt(createdAt)
                .updatedAt(Instant.now())
                .build();
    }

    public User delete() {
        return User.builder()
                .userId(userId)
                .email(email)
                .password(password)
                .nickname(nickname)
                .name(name)
                .profileImageUrl(profileImageUrl)
                .point(point)
                .role(role)
                .grade(grade)
                .status(UserStatus.DELETED)
                .createdAt(createdAt)
                .updatedAt(Instant.now())
                .build();
    }

    private static void validateRequired(
            String email,
            String password,
            String nickname,
            String name,
            Long point,
            UserRole role,
            UserGrade grade,
            UserStatus status
    ) {
        if (email == null || email.isBlank()) {
            throw new UserDomainException(UserErrorCode.INVALID_EMAIL);
        }
        if (password == null || password.isBlank()) {
            throw new UserDomainException(UserErrorCode.INVALID_PASSWORD);
        }
        if (nickname == null || nickname.isBlank()) {
            throw new UserDomainException(UserErrorCode.INVALID_NICKNAME);
        }
        if (name == null || name.isBlank()) {
            throw new UserDomainException(UserErrorCode.INVALID_NAME);
        }
        if (point == null || point < 0) {
            throw new UserDomainException(UserErrorCode.INVALID_POINT);
        }
        if (role == null) {
            throw new UserDomainException(UserErrorCode.INVALID_USER_ROLE);
        }
        if (grade == null) {
            throw new UserDomainException(UserErrorCode.INVALID_USER_GRADE);
        }
        if (status == null) {
            throw new UserDomainException(UserErrorCode.INVALID_USER_STATUS);
        }
    }

    public record UserId(Long id) {
        public UserId {
            if (id == null || id <= 0) {
                throw new UserDomainException(UserErrorCode.INVALID_ID);
            }
        }
    }
}
