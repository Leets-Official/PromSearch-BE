package com.promsearch.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.promsearch.user.domain.enums.UserGrade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserTest {

    @DisplayName("회원가입 직후 기본 등급은 Node이다")
    @Test
    void createStartsAtNodeGrade() {
        User user = User.create("user@promsearch.com", "password", "nickname", "name", null);

        assertThat(user.getGrade()).isEqualTo(UserGrade.NODE);
    }
}
