package com.promsearch.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.promsearch.user.domain.User.UserId;
import com.promsearch.user.domain.enums.UserGrade;
import com.promsearch.user.domain.enums.UserRole;
import com.promsearch.user.domain.enums.UserStatus;
import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserTest {

    @DisplayName("회원가입 직후 기본 등급은 Node이다")
    @Test
    void createStartsAtNodeGrade() {
        User user = User.create("user@promsearch.com", "password", "nickname", "name", null);

        assertThat(user.getGrade()).isEqualTo(UserGrade.NODE);
    }

    @DisplayName("Prime 유저는 Origin으로 승급할 수 있다")
    @Test
    void promoteToOriginFromPrime() {
        User user = userWithGrade(UserGrade.PRIME);

        User promoted = user.promoteToOrigin();

        assertThat(promoted.getGrade()).isEqualTo(UserGrade.ORIGIN);
    }

    @DisplayName("Prime이 아닌 유저는 Origin으로 승급할 수 없다")
    @Test
    void rejectsPromotionFromNonPrimeGrade() {
        User user = userWithGrade(UserGrade.CORE);

        assertThatThrownBy(user::promoteToOrigin)
                .isInstanceOf(UserDomainException.class)
                .extracting("baseCode")
                .isEqualTo(UserErrorCode.INVALID_GRADE_TRANSITION);
    }

    private User userWithGrade(UserGrade grade) {
        Instant now = Instant.now();
        return User.reconstruct(
                new UserId(1L),
                "user@promsearch.com",
                "password",
                "nickname",
                "name",
                null,
                null,
                0L,
                UserRole.USER,
                grade,
                UserStatus.ACTIVE,
                now,
                now
        );
    }
}
