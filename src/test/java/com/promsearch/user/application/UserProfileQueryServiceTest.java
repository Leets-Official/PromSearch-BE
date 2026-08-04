package com.promsearch.user.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.promsearch.user.application.port.out.user.LoadUserPort;
import com.promsearch.user.application.port.out.user.UserProfileStats;
import com.promsearch.user.application.port.out.user.UserProfileStatsReader;
import com.promsearch.user.application.service.query.UserProfileQueryService;
import com.promsearch.user.application.usecase.dto.PublicUserProfileInfo;
import com.promsearch.user.domain.User;
import com.promsearch.user.domain.User.UserId;
import com.promsearch.user.domain.enums.UserGrade;
import com.promsearch.user.domain.enums.UserRole;
import com.promsearch.user.domain.enums.UserStatus;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserProfileQueryServiceTest {

    private FakeUserRepository userRepository;
    private FakeUserProfileStatsReader userProfileStatsReader;
    private UserProfileQueryService userProfileQueryService;

    @BeforeEach
    void setUp() {
        userRepository = new FakeUserRepository();
        userProfileStatsReader = new FakeUserProfileStatsReader();
        userProfileQueryService = new UserProfileQueryService(userRepository, userProfileStatsReader);
    }

    @Test
    void getProfileReturnsOnlyPublicProfileAndAggregatedStats() {
        userRepository.user = testUser();
        userProfileStatsReader.stats = new UserProfileStats(5, 42, 120);

        PublicUserProfileInfo profile = userProfileQueryService.getProfile(1L);

        assertThat(profile.userId()).isEqualTo(1L);
        assertThat(profile.nickname()).isEqualTo("creator");
        assertThat(profile.profileImageUrl()).isEqualTo("https://cdn.test/profile.png");
        assertThat(profile.grade()).isEqualTo(UserGrade.PRIME);
        assertThat(profile.promptCount()).isEqualTo(5);
        assertThat(profile.totalLikeCount()).isEqualTo(42);
        assertThat(profile.totalViewCount()).isEqualTo(120);
    }

    private User testUser() {
        Instant now = Instant.now();
        return User.reconstruct(
                new UserId(1L),
                "creator@example.com",
                "encoded-password",
                "creator",
                "Creator Name",
                "https://cdn.test/profile.png",
                null,
                100L,
                UserRole.USER,
                UserGrade.PRIME,
                UserStatus.ACTIVE,
                now,
                now
        );
    }

    private static class FakeUserRepository implements LoadUserPort {

        private User user;

        @Override
        public User getById(Long userId) {
            return user;
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return Optional.empty();
        }

        @Override
        public Optional<User> findById(Long userId) {
            return Optional.ofNullable(user);
        }

        @Override
        public boolean existsByNickname(String nickname) {
            return false;
        }

        @Override
        public boolean existsByEmail(String email) {
            return false;
        }
    }

    private static class FakeUserProfileStatsReader implements UserProfileStatsReader {

        private UserProfileStats stats = UserProfileStats.empty();

        @Override
        public UserProfileStats getByUserId(Long userId) {
            return stats;
        }
    }
}
