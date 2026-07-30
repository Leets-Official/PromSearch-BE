package com.promsearch.user.infrastructure.persistence.entity;

import com.promsearch.common.BaseEntity;
import com.promsearch.user.domain.User;
import com.promsearch.user.domain.User.UserId;
import com.promsearch.user.domain.enums.UserGrade;
import com.promsearch.user.domain.enums.UserRole;
import com.promsearch.user.domain.enums.UserStatus;
import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class UserJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "nickname", nullable = false, unique = true, length = 100)
    private String nickname;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "profile_image_url", columnDefinition = "TEXT")
    private String profileImageUrl;

    @Column(name = "points", nullable = false)
    private Long points;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade", nullable = false, length = 20)
    private UserGrade grade;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    @Builder(access = AccessLevel.PRIVATE)
    private UserJpaEntity(String email, String password, String nickname, String name, String profileImageUrl) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.points = 0L;
        this.role = UserRole.USER;
        this.grade = UserGrade.NORMAL;
        this.status = UserStatus.ACTIVE;
    }

    public static UserJpaEntity create(String email, String password, String nickname, String name, String profileImageUrl) {
        return UserJpaEntity.builder()
                .email(email)
                .password(password)
                .nickname(nickname)
                .name(name)
                .profileImageUrl(profileImageUrl)
                .build();
    }

    public void updateFrom(User user) {
        validateProfile(user.getEmail(), user.getPassword(), user.getNickname(), user.getName());

        this.email = user.getEmail();
        this.password = user.getPassword();
        this.nickname = user.getNickname();
        this.name = user.getName();
        this.profileImageUrl = user.getProfileImageUrl();
        this.points = user.getPoint();
        this.role = user.getRole();
        this.grade = user.getGrade();
        this.status = user.getStatus();

        if (user.getStatus() == UserStatus.DELETED) {
            markDeleted();
        }
    }

    public User toDomain() {
        return User.reconstruct(
                new UserId(id),
                email,
                password,
                nickname,
                name,
                profileImageUrl,
                points,
                role,
                grade,
                status,
                getCreatedAt(),
                getUpdatedAt()
        );
    }

    public String getPassword() {
        return password;
    }

    private void validateProfile(String email, String password, String nickname, String name) {
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
    }
}
