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
    private final String profileImageObjectKey;
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
            String profileImageObjectKey,
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
        this.profileImageObjectKey = profileImageObjectKey;
        this.point = point;
        this.role = role;
        this.grade = grade;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static User create(String email, String password, String nickname, String name, String profileImageUrl) {
        validateRequired(email, password, nickname, 0L, UserRole.USER, UserGrade.NORMAL, UserStatus.ACTIVE);

        Instant now = Instant.now();
        return User.builder()
                .email(email)
                .password(password)
                .nickname(nickname)
                .name(name)
                .profileImageUrl(profileImageUrl)
                .profileImageObjectKey(null)
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
            String profileImageObjectKey,
            Long point,
            UserRole role,
            UserGrade grade,
            UserStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        validateRequired(email, password, nickname, point, role, grade, status);

        return User.builder()
                .userId(userId)
                .email(email)
                .password(password)
                .nickname(nickname)
                .name(name)
                .profileImageUrl(profileImageUrl)
                .profileImageObjectKey(profileImageObjectKey)
                .point(point)
                .role(role)
                .grade(grade)
                .status(status)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    public User updateProfile(String email, String nickname, String name) {
        validateRequired(email, password, nickname, point, role, grade, status);

        return User.builder()
                .userId(userId)
                .email(email)
                .password(password)
                .nickname(nickname)
                .name(name)
                .profileImageUrl(profileImageUrl)
                .profileImageObjectKey(profileImageObjectKey)
                .point(point)
                .role(role)
                .grade(grade)
                .status(status)
                .createdAt(createdAt)
                .updatedAt(Instant.now())
                .build();
    }

    /**
     * 사용자가 직접 업로드한 이미지를 현재 프로필 이미지로 변경한다.
     *
     * <p>S3 관리 이미지를 선택한 시점부터 소셜 로그인 제공자의 외부 URL은 사용하지 않으므로
     * 외부 URL을 제거하고 객체 키만 유지한다.</p>
     *
     * @param objectKey 검증이 끝난 프로필 이미지 객체 키
     * @return S3 프로필 이미지 상태로 변경된 새 사용자 객체
     */
    public User changeProfileImage(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new UserDomainException(UserErrorCode.PROFILE_IMAGE_UPLOAD_NOT_FOUND);
        }
        return User.builder()
                .userId(userId)
                .email(email)
                .password(password)
                .nickname(nickname)
                .name(name)
                .profileImageUrl(null)
                .profileImageObjectKey(objectKey)
                .point(point)
                .role(role)
                .grade(grade)
                .status(status)
                .createdAt(createdAt)
                .updatedAt(Instant.now())
                .build();
    }

    public User changeExternalProfileImage(String profileImageUrl) {
        if (profileImageUrl == null || profileImageUrl.isBlank()) {
            throw new UserDomainException(UserErrorCode.PROFILE_IMAGE_UPLOAD_NOT_FOUND);
        }
        return User.builder()
                .userId(userId)
                .email(email)
                .password(password)
                .nickname(nickname)
                .name(name)
                .profileImageUrl(profileImageUrl)
                .profileImageObjectKey(null)
                .point(point)
                .role(role)
                .grade(grade)
                .status(status)
                .createdAt(createdAt)
                .updatedAt(Instant.now())
                .build();
    }

    /**
     * 소셜 제공자 URL과 직접 업로드한 객체 키를 모두 제거한다.
     *
     * @return 프로필 이미지 연결이 없는 새 사용자 객체
     */
    public User removeProfileImage() {
        return User.builder()
                .userId(userId)
                .email(email)
                .password(password)
                .nickname(nickname)
                .name(name)
                .profileImageUrl(null)
                .profileImageObjectKey(null)
                .point(point)
                .role(role)
                .grade(grade)
                .status(status)
                .createdAt(createdAt)
                .updatedAt(Instant.now())
                .build();
    }

    public User changePassword(String password) {
        validateRequired(email, password, nickname, point, role, grade, status);

        return User.builder()
                .userId(userId)
                .email(email)
                .password(password)
                .nickname(nickname)
                .name(name)
                .profileImageUrl(profileImageUrl)
                .profileImageObjectKey(profileImageObjectKey)
                .point(point)
                .role(role)
                .grade(grade)
                .status(status)
                .createdAt(createdAt)
                .updatedAt(Instant.now())
                .build();
    }

    public User delete() {
        Instant now = Instant.now();
        String deletedPrefix = "deleted_" + userId.id() + "_" + now.toEpochMilli();

        return User.builder()
                .userId(userId)
                .email(deletedPrefix + "@deleted.promsearch")
                .password(deletedPrefix)
                .nickname(deletedPrefix + "_user")
                .name("Deleted User")
                .profileImageUrl(null)
                .profileImageObjectKey(null)
                .point(point)
                .role(role)
                .grade(grade)
                .status(UserStatus.DELETED)
                .createdAt(createdAt)
                .updatedAt(now)
                .build();
    }

    private static void validateRequired(
            String email,
            String password,
            String nickname,
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
        if (status != UserStatus.DELETED) {
            NicknamePolicy.validate(nickname);
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
