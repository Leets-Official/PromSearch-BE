package com.promsearch.auth.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.promsearch.auth.domain.exception.AuthDomainException;
import com.promsearch.auth.domain.exception.AuthErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class CredentialPolicyTest {

    @DisplayName("일반적으로 사용하는 이메일 형식은 허용한다")
    @ParameterizedTest
    @ValueSource(strings = {
            "user@example.com",
            "user.name@example.com",
            "user+tag@example.co.kr",
            "user_name@example.com",
            "user-name@sub.example.com"
    })
    void acceptsValidEmail(String email) {
        assertThatCode(() -> CredentialPolicy.validateEmail(email))
                .doesNotThrowAnyException();
    }

    @DisplayName("형식이 올바르지 않은 이메일은 거부한다")
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "",
            "user",
            "user@",
            "@example.com",
            "user@example",
            "user name@example.com",
            "한글@example.com"
    })
    void rejectsInvalidEmail(String email) {
        assertPolicyViolation(() -> CredentialPolicy.validateEmail(email), AuthErrorCode.INVALID_EMAIL_FORMAT);
    }

    @DisplayName("255자를 초과하는 이메일은 거부한다")
    @Test
    void rejectsTooLongEmail() {
        String email = "a".repeat(CredentialPolicy.EMAIL_MAX_LENGTH - 4) + "@a.io";

        assertPolicyViolation(() -> CredentialPolicy.validateEmail(email), AuthErrorCode.INVALID_EMAIL_FORMAT);
    }

    @DisplayName("영문, 숫자, 특수문자 중 2가지 이상을 조합한 8~20자 비밀번호는 허용한다")
    @ParameterizedTest
    @ValueSource(strings = {
            "password1",
            "password!",
            "1234567!",
            "Pass123!",
            "abcdefghij1234567890"
    })
    void acceptsValidPassword(String password) {
        assertThatCode(() -> CredentialPolicy.validatePassword(password))
                .doesNotThrowAnyException();
    }

    @DisplayName("비밀번호 정책을 충족하지 않으면 거부한다")
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "",
            "abc123!",
            "abcdefghij12345678901",
            "password",
            "12345678",
            "!@#$%^&*",
            "password 1",
            "비밀번호123456"
    })
    void rejectsInvalidPassword(String password) {
        assertPolicyViolation(
                () -> CredentialPolicy.validatePassword(password),
                AuthErrorCode.PASSWORD_POLICY_VIOLATION
        );
    }

    private void assertPolicyViolation(Runnable validation, AuthErrorCode expectedErrorCode) {
        assertThatThrownBy(validation::run)
                .isInstanceOf(AuthDomainException.class)
                .extracting("baseCode")
                .isEqualTo(expectedErrorCode);
    }
}
