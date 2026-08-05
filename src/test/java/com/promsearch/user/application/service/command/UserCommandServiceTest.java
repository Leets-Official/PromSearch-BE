package com.promsearch.user.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.promsearch.user.application.port.out.user.LoadUserPort;
import com.promsearch.user.application.port.out.user.SaveUserInterestTagPort;
import com.promsearch.user.application.port.out.user.SaveUserPort;
import com.promsearch.user.application.usecase.dto.BootstrapAdminAccountCommand;
import com.promsearch.user.application.usecase.dto.ChangePasswordCommand;
import com.promsearch.user.application.usecase.dto.SignupCommand;
import com.promsearch.user.application.usecase.dto.SignupInfo;
import com.promsearch.user.application.usecase.dto.UpdateUserProfileCommand;
import com.promsearch.user.application.usecase.dto.UserInfo;
import com.promsearch.user.domain.User;
import com.promsearch.user.domain.User.UserId;
import com.promsearch.user.domain.enums.UserGrade;
import com.promsearch.user.domain.enums.UserRole;
import com.promsearch.user.domain.enums.UserStatus;
import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserCommandServiceTest {

    private FakeUserRepository userRepository;
    private FakeSaveUserInterestTagPort saveUserInterestTagPort;
    private UserCommandService userCommandService;

    @BeforeEach
    void setUp() {
        userRepository = new FakeUserRepository();
        saveUserInterestTagPort = new FakeSaveUserInterestTagPort();
        userCommandService = new UserCommandService(
                userRepository,
                userId -> List.of(),
                userRepository,
                (type, tagIds) -> tagIds,
                saveUserInterestTagPort,
                (userId, agreements) -> {
                },
                new TestPasswordEncoder(),
                (externalUrl, objectKey) -> objectKey == null ? externalUrl : "signed:" + objectKey,
                objectKey -> {
                }
        );
    }

    @Test
    void updateProfileChangesMemberInformation() {
        userRepository.save(testUser(
                1L,
                "old@example.com",
                "old-password",
                "oldNick",
                "oldName",
                "https://image.test/original.png",
                UserStatus.ACTIVE
        ));

        UserInfo userInfo = userCommandService.updateProfile(
                UpdateUserProfileCommand.of(
                        1L,
                        " newNick ",
                        " new@example.com ",
                        null,
                        null
                )
        );

        assertThat(userInfo.nickname()).isEqualTo("newNick");
        assertThat(userInfo.email()).isEqualTo("new@example.com");
        assertThat(userInfo.profileImageUrl()).isEqualTo("https://image.test/original.png");
        assertThat(userRepository.users.get(1L).getPassword()).isEqualTo("old-password");
        assertThat(saveUserInterestTagPort.lastReplacedUserId).isNull();
    }

    @Test
    void updateProfileReplacesInterestTagsWhenBothProvided() {
        userRepository.save(testUser(1L, "old@example.com", "old-password", "oldNick", "oldName", null, UserStatus.ACTIVE));

        userCommandService.updateProfile(
                UpdateUserProfileCommand.of(1L, null, null, List.of(1L), List.of(2L, 3L))
        );

        assertThat(saveUserInterestTagPort.lastReplacedUserId).isEqualTo(1L);
        assertThat(saveUserInterestTagPort.lastReplacedTagIds).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void updateProfileAllowsPartialInterestTagUpdate() {
        assertThatCode(() -> UpdateUserProfileCommand.of(1L, null, null, List.of(1L), null))
                .doesNotThrowAnyException();
    }

    @Test
    void updateProfileKeepsCurrentValuesWhenRequestFieldsAreNull() {
        userRepository.save(testUser(1L, "old@example.com", "old-password", "oldNick", "oldName", "old-image", UserStatus.ACTIVE));

        UserInfo userInfo = userCommandService.updateProfile(
                UpdateUserProfileCommand.of(1L, null, null, null, null)
        );

        assertThat(userInfo.email()).isEqualTo("old@example.com");
        assertThat(userInfo.nickname()).isEqualTo("oldNick");
        assertThat(userInfo.profileImageUrl()).isEqualTo("old-image");
        assertThat(userRepository.users.get(1L).getPassword()).isEqualTo("old-password");
    }

    @Test
    void updateProfileRejectsDuplicateNickname() {
        userRepository.save(testUser(1L, "user1@example.com", "password", "one", "one", null, UserStatus.ACTIVE));
        userRepository.save(testUser(2L, "user2@example.com", "password", "two", "two", null, UserStatus.ACTIVE));

        assertThatThrownBy(() -> userCommandService.updateProfile(
                UpdateUserProfileCommand.of(1L, "two", null, null, null)
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
                UpdateUserProfileCommand.of(1L, null, "user2@example.com", null, null)
        ))
                .isInstanceOf(UserDomainException.class)
                .extracting("baseCode")
                .isEqualTo(UserErrorCode.DUPLICATE_EMAIL);
    }

    @Test
    void changePasswordEncodesNewPasswordWhenCurrentPasswordMatches() {
        userRepository.save(testUser(
                1L,
                "old@example.com",
                "encoded:old-password",
                "oldNick",
                "oldName",
                null,
                UserStatus.ACTIVE
        ));

        userCommandService.changePassword(ChangePasswordCommand.of(1L, "old-password", "new-password"));

        assertThat(userRepository.users.get(1L).getPassword()).isEqualTo("encoded:new-password");
    }

    @Test
    void changePasswordRejectsMismatchedCurrentPassword() {
        userRepository.save(testUser(
                1L,
                "old@example.com",
                "encoded:old-password",
                "oldNick",
                "oldName",
                null,
                UserStatus.ACTIVE
        ));

        assertThatThrownBy(() -> userCommandService.changePassword(
                ChangePasswordCommand.of(1L, "wrong-password", "new-password")
        ))
                .isInstanceOf(UserDomainException.class)
                .extracting("baseCode")
                .isEqualTo(UserErrorCode.INVALID_PASSWORD);
    }

    @Test
    void deleteMarksUserAsDeletedAndAnonymizesUniqueInformation() {
        userRepository.save(testUser(1L, "old@example.com", "password", "oldNick", "oldName", null, UserStatus.ACTIVE));

        userCommandService.delete(1L);

        User deletedUser = userRepository.users.get(1L);
        assertThat(deletedUser.getStatus()).isEqualTo(UserStatus.DELETED);
        assertThat(deletedUser.getEmail()).startsWith("deleted_1_").endsWith("@deleted.promsearch");
        assertThat(deletedUser.getNickname()).startsWith("deleted_1_").endsWith("_user");
        assertThat(deletedUser.getName()).isEqualTo("Deleted User");
        assertThat(deletedUser.getProfileImageUrl()).isNull();
    }

    @Test
    void signupAllowsReusingEmailAndNicknameAfterDelete() {
        userRepository.save(testUser(1L, "old@example.com", "password", "oldNick", "oldName", null, UserStatus.ACTIVE));

        userCommandService.delete(1L);
        SignupInfo signupInfo = userCommandService.signup(
                SignupCommand.of("oldNick", "old@example.com", "new-password")
        );

        assertThat(signupInfo.userId()).isEqualTo(2L);
        assertThat(signupInfo.email()).isEqualTo("old@example.com");
        assertThat(signupInfo.nickname()).isEqualTo("oldNick");
    }

    @Test
    void bootstrapCreatesAdminAccountWhenNotExists() {
        userCommandService.bootstrap(BootstrapAdminAccountCommand.of("admin@example.com", "Admin1234!", "admin"));

        User admin = userRepository.users.values().iterator().next();
        assertThat(admin.getEmail()).isEqualTo("admin@example.com");
        assertThat(admin.getNickname()).isEqualTo("admin");
        assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(admin.getPassword()).isEqualTo("encoded:Admin1234!");
    }

    @Test
    void bootstrapPromotesExistingUserToAdminWhenRoleIsNotAdmin() {
        userRepository.save(testUser(1L, "admin@example.com", "old-password", "admin", "admin", null, UserStatus.ACTIVE));

        userCommandService.bootstrap(BootstrapAdminAccountCommand.of("admin@example.com", "Admin1234!", "admin"));

        assertThat(userRepository.users).hasSize(1);
        User promoted = userRepository.users.get(1L);
        assertThat(promoted.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(promoted.getPassword()).isEqualTo("old-password");
    }

    @Test
    void bootstrapSkipsWhenAdminAlreadyExistsWithAdminRole() {
        User existingAdmin = User.reconstruct(
                new UserId(1L),
                "admin@example.com",
                "old-password",
                "admin",
                "admin",
                null,
                null,
                0L,
                UserRole.ADMIN,
                UserGrade.NODE,
                UserStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );
        userRepository.save(existingAdmin);

        userCommandService.bootstrap(BootstrapAdminAccountCommand.of("admin@example.com", "Admin1234!", "admin"));

        assertThat(userRepository.users).hasSize(1);
        assertThat(userRepository.users.get(1L).getPassword()).isEqualTo("old-password");
    }

    @Test
    void bootstrapSkipsWhenEmailOrPasswordBlank() {
        userCommandService.bootstrap(BootstrapAdminAccountCommand.of("", "Admin1234!", "admin"));
        userCommandService.bootstrap(BootstrapAdminAccountCommand.of("admin@example.com", " ", "admin"));

        assertThat(userRepository.users).isEmpty();
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
                null,
                0L,
                UserRole.USER,
                UserGrade.NODE,
                status,
                now,
                now
        );
    }

    private static class FakeUserRepository implements LoadUserPort, SaveUserPort {

        private final Map<Long, User> users = new HashMap<>();
        private long nextId = 1L;

        void save(User user) {
            create(user);
        }

        @Override
        public User create(User user) {
            Long userId = user.getUserId() == null ? nextId++ : user.getUserId().id();
            nextId = Math.max(nextId, userId + 1);
            User savedUser = User.reconstruct(
                    new UserId(userId),
                    user.getEmail(),
                    user.getPassword(),
                    user.getNickname(),
                    user.getName(),
                    user.getProfileImageUrl(),
                    user.getProfileImageObjectKey(),
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
        public Optional<User> findByEmail(String email) {
            return users.values().stream()
                    .filter(user -> user.getEmail().equals(email))
                    .findFirst();
        }

        @Override
        public Optional<User> findById(Long userId) {
            return Optional.ofNullable(users.get(userId));
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
        public User update(User user) {
            Long userId = user.getUserId().id();
            getById(userId);
            users.put(userId, user);
            return user;
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

    private static class FakeSaveUserInterestTagPort implements SaveUserInterestTagPort {

        private Long lastReplacedUserId;
        private List<Long> lastReplacedTagIds;

        @Override
        public void save(Long userId, List<Long> tagIds) {
        }

        @Override
        public void replace(Long userId, List<Long> tagIds) {
            lastReplacedUserId = userId;
            lastReplacedTagIds = tagIds;
        }
    }
}
