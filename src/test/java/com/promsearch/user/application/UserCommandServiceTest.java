package com.promsearch.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.promsearch.user.application.port.out.UserRepository;
import com.promsearch.user.domain.User;
import com.promsearch.user.domain.User.UserId;
import com.promsearch.user.domain.enums.UserGrade;
import com.promsearch.user.domain.enums.UserRole;
import com.promsearch.user.domain.enums.UserStatus;
import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserCommandServiceTest {

    private FakeUserRepository userRepository;
    private UserCommandService userCommandService;

    @BeforeEach
    void setUp() {
        userRepository = new FakeUserRepository();
        userCommandService = new UserCommandService(userRepository, new TestPasswordEncoder());
    }

    @Test
    void updateProfileChangesMemberInformation() {
        userRepository.save(testUser(1L, "old@example.com", "old-password", "oldNick", "oldName", null, UserStatus.ACTIVE));

        UserInfo userInfo = userCommandService.updateProfile(
                UpdateUserProfileCommand.of(
                        1L,
                        " newName ",
                        " newNick ",
                        " new@example.com ",
                        "new-password",
                        " https://image.test/me.png "
                )
        );

        assertThat(userInfo.name()).isEqualTo("newName");
        assertThat(userInfo.nickname()).isEqualTo("newNick");
        assertThat(userInfo.email()).isEqualTo("new@example.com");
        assertThat(userInfo.profileImageUrl()).isEqualTo("https://image.test/me.png");
        assertThat(userRepository.users.get(1L).getPassword()).isEqualTo("encoded:new-password");
    }

    @Test
    void updateProfileKeepsCurrentValuesWhenRequestFieldsAreNull() {
        userRepository.save(testUser(1L, "old@example.com", "old-password", "oldNick", "oldName", "old-image", UserStatus.ACTIVE));

        UserInfo userInfo = userCommandService.updateProfile(
                UpdateUserProfileCommand.of(1L, null, null, null, null, null)
        );

        assertThat(userInfo.email()).isEqualTo("old@example.com");
        assertThat(userInfo.nickname()).isEqualTo("oldNick");
        assertThat(userInfo.name()).isEqualTo("oldName");
        assertThat(userInfo.profileImageUrl()).isEqualTo("old-image");
        assertThat(userRepository.users.get(1L).getPassword()).isEqualTo("old-password");
    }

    @Test
    void updateProfileRejectsDuplicateNickname() {
        userRepository.save(testUser(1L, "user1@example.com", "password", "one", "one", null, UserStatus.ACTIVE));
        userRepository.save(testUser(2L, "user2@example.com", "password", "two", "two", null, UserStatus.ACTIVE));

        assertThatThrownBy(() -> userCommandService.updateProfile(
                UpdateUserProfileCommand.of(1L, null, "two", null, null, null)
        ))
                .isInstanceOf(UserDomainException.class)
                .extracting("baseCode")
                .isEqualTo(UserErrorCode.DUPLICATE_NICKNAME);
    }

    @Test
    void updateProfileRejectsDuplicateEmail() {
        userRepository.save(testUser(1L, "user1@example.com", "password", "one", "one", null, UserStatus.ACTIVE));
        userRepository.save(testUser(2L, "user2@example.com", "password", "two", "two", null, UserStatus.ACTIVE));

        assertThatThrownBy(() -> userCommandService.updateProfile(
                UpdateUserProfileCommand.of(1L, null, null, "user2@example.com", null, null)
        ))
                .isInstanceOf(UserDomainException.class)
                .extracting("baseCode")
                .isEqualTo(UserErrorCode.DUPLICATE_EMAIL);
    }

    @Test
    void deleteMarksUserAsDeleted() {
        userRepository.save(testUser(1L, "old@example.com", "password", "oldNick", "oldName", null, UserStatus.ACTIVE));

        userCommandService.delete(1L);

        assertThat(userRepository.users.get(1L).getStatus()).isEqualTo(UserStatus.DELETED);
    }

    private User testUser(
            Long userId,
            String email,
            String password,
            String nickname,
            String name,
            String profileImageUrl,
            UserStatus status
    ) {
        Instant now = Instant.now();
        return User.reconstruct(
                new UserId(userId),
                email,
                password,
                nickname,
                name,
                profileImageUrl,
                0L,
                UserRole.USER,
                UserGrade.NORMAL,
                status,
                now,
                now
        );
    }

    private static class FakeUserRepository implements UserRepository {

        private final Map<Long, User> users = new HashMap<>();
        private long nextId = 1L;

        void save(User user) {
            create(user);
        }

        @Override
        public User create(User user) {
            Long userId = user.getUserId() == null ? nextId++ : user.getUserId().id();
            User savedUser = User.reconstruct(
                    new UserId(userId),
                    user.getEmail(),
                    user.getPassword(),
                    user.getNickname(),
                    user.getName(),
                    user.getProfileImageUrl(),
                    user.getPoint(),
                    user.getRole(),
                    user.getGrade(),
                    user.getStatus(),
                    user.getCreatedAt(),
                    user.getUpdatedAt()
            );
            users.put(userId, savedUser);
            return savedUser;
        }

        @Override
        public User getById(Long userId) {
            User user = users.get(userId);
            if (user == null || user.getStatus() != UserStatus.ACTIVE) {
                throw new UserDomainException(UserErrorCode.USER_NOT_FOUND);
            }
            return user;
        }

        @Override
        public boolean existsByNickname(String nickname) {
            return users.values().stream()
                    .anyMatch(user -> user.getStatus() != UserStatus.DELETED
                            && user.getNickname().equals(nickname));
        }

        @Override
        public boolean existsByEmail(String email) {
            return users.values().stream()
                    .anyMatch(user -> user.getStatus() != UserStatus.DELETED
                            && user.getEmail().equals(email));
        }

        @Override
        public User updateProfile(
                Long userId,
                String email,
                String password,
                String nickname,
                String name,
                String profileImageUrl
        ) {
            User user = getById(userId).updateProfile(email, password, nickname, name, profileImageUrl);
            users.put(userId, user);
            return user;
        }

        @Override
        public void deleteById(Long userId) {
            User user = getById(userId).delete();
            users.put(userId, user);
        }
    }

    private static class TestPasswordEncoder implements PasswordEncoder {

        @Override
        public String encode(CharSequence rawPassword) {
            return "encoded:" + rawPassword;
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return encodedPassword.equals(encode(rawPassword));
        }
    }
}
