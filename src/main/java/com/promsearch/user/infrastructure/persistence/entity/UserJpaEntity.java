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

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "profile_image_url", columnDefinition = "TEXT")
    private String profileImageUrl;

    @Column(name = "profile_image_object_key", length = 1024)
    private String profileImageObjectKey;

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
    private UserJpaEntity(
            String email,
            String password,
            String nickname,
            String name,
            String profileImageUrl,
            String profileImageObjectKey
    ) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.profileImageObjectKey = profileImageObjectKey;
        this.points = 0L;
        this.role = UserRole.USER;
        this.grade = UserGrade.NODE;
        this.status = UserStatus.ACTIVE;
    }

    /**
     * 게시글 작성에 따른 자동 승급을 1단계 적용한다. Origin은 자동 승급 대상이 아니므로
     * Prime 이상인 경우 아무 변화가 없다.
     *
     * @return 이번 호출로 처음 Prime 등급에 도달했는지 여부
     */
    public boolean promoteGrade() {
        return grade.nextAutoPromotionGrade()
                .map(nextGrade -> {
                    this.grade = nextGrade;
                    return nextGrade == UserGrade.PRIME;
                })
                .orElse(false);
    }

    public static UserJpaEntity create(
            String email,
            String password,
            String nickname,
            String name,
            String profileImageUrl,
            String profileImageObjectKey
    ) {
        return UserJpaEntity.builder()
                .email(email)
                .password(password)
                .nickname(nickname)
                .name(name)
                .profileImageUrl(profileImageUrl)
                .profileImageObjectKey(profileImageObjectKey)
                .build();
    }

    public static UserJpaEntity create(
            String email,
            String password,
            String nickname,
            String name,
            String profileImageUrl
    ) {
        return create(email, password, nickname, name, profileImageUrl, null);
    }

    public void updateFrom(User user) {
        validateProfile(user.getEmail(), user.getPassword(), user.getNickname());

        this.email = user.getEmail();
        this.password = user.getPassword();
        this.nickname = user.getNickname();
        this.name = user.getName();
        this.profileImageUrl = user.getProfileImageUrl();
        this.profileImageObjectKey = user.getProfileImageObjectKey();
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
                profileImageObjectKey,
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

    public Long getId() {
        return id;
    }

    private void validateProfile(String email, String password, String nickname) {
        if (email == null || email.isBlank()) {
            throw new UserDomainException(UserErrorCode.INVALID_EMAIL);
        }
        if (password == null || password.isBlank()) {
            throw new UserDomainException(UserErrorCode.INVALID_PASSWORD);
        }
        if (nickname == null || nickname.isBlank()) {
            throw new UserDomainException(UserErrorCode.INVALID_NICKNAME);
        }
    }
}
