package com.promsearch.user.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.promsearch.user.application.port.out.user.LoadUserPort;
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
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserQueryServiceTest {

    private FakeUserLoadPort loadUserPort;
    private UserQueryService userQueryService;

    @BeforeEach
    void setUp() {
        loadUserPort = new FakeUserLoadPort();
        userQueryService = new UserQueryService(loadUserPort);
    }

    @Test
    void getMyProfileReturnsActiveUserInfo() {
        loadUserPort.save(testUser(1L, "hanharam@example.com", "hanharam", UserGrade.ORIGIN, 1200L, UserStatus.ACTIVE));

        UserInfo userInfo = userQueryService.getMyProfile(1L);

        assertThat(userInfo.nickname()).isEqualTo("hanharam");
        assertThat(userInfo.email()).isEqualTo("hanharam@example.com");
        assertThat(userInfo.point()).isEqualTo(1200L);
        assertThat(userInfo.grade()).isEqualTo(UserGrade.ORIGIN);
    }

    @Test
    void getMyProfileRejectsMissingUser() {
        assertThatThrownBy(() -> userQueryService.getMyProfile(1L))
                .isInstanceOf(UserDomainException.class)
                .extracting("baseCode")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    @Test
    void getMyProfileRejectsDeletedUser() {
        loadUserPort.save(testUser(1L, "old@example.com", "oldNick", UserGrade.NORMAL, 0L, UserStatus.DELETED));

        assertThatThrownBy(() -> userQueryService.getMyProfile(1L))
                .isInstanceOf(UserDomainException.class)
                .extracting("baseCode")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    private User testUser(Long userId, String email, String nickname, UserGrade grade, Long point, UserStatus status) {
        Instant now = Instant.now();
        return User.reconstruct(
                new UserId(userId),
                email,
                "password",
                nickname,
                "name",
                null,
                point,
                UserRole.USER,
                grade,
                status,
                now,
                now
        );
    }

    private static class FakeUserLoadPort implements LoadUserPort {

        private final Map<Long, User> users = new HashMap<>();

        void save(User user) {
            users.put(user.getUserId().id(), user);
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
            return users.values().stream().filter(user -> user.getEmail().equals(email)).findFirst();
        }

        @Override
        public Optional<User> findById(Long userId) {
            return Optional.ofNullable(users.get(userId));
        }

        @Override
        public boolean existsByNickname(String nickname) {
            return users.values().stream().anyMatch(user -> user.getNickname().equals(nickname));
        }

        @Override
        public boolean existsByEmail(String email) {
            return users.values().stream().anyMatch(user -> user.getEmail().equals(email));
        }
    }
}
