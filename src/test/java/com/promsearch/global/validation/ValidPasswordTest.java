package com.promsearch.global.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.promsearch.auth.interfaces.dto.SignupRequest;
import com.promsearch.user.interfaces.dto.ChangePasswordRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ValidPasswordTest {

    private static final String ERROR_MESSAGE =
            "비밀번호는 영문, 숫자, 특수문자 중 2가지 이상을 조합하여 8자 이상 20자 이하여야 합니다.";

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @ParameterizedTest(name = "회원가입 비밀번호 허용: {0}")
    @ValueSource(strings = {
            "password1",
            "password!",
            "1234567!",
            "Pass123!",
            "abcdefghij1234567890"
    })
    void signupAcceptsValidPassword(String password) {
        SignupRequest request = new SignupRequest("홍길동", "gildong", "gildong@example.com", password);

        assertThat(validator.validate(request)).isEmpty();
    }

    @ParameterizedTest(name = "비밀번호 변경 시 새 비밀번호 허용: {0}")
    @ValueSource(strings = {
            "password1",
            "password!",
            "1234567!",
            "Pass123!",
            "abcdefghij1234567890"
    })
    void changePasswordAcceptsValidNewPassword(String password) {
        ChangePasswordRequest request = new ChangePasswordRequest("oldPassword123!", password);

        assertThat(validator.validate(request)).isEmpty();
    }

    @ParameterizedTest(name = "회원가입 비밀번호 거부: {0}")
    @ValueSource(strings = {
            "abc123!",
            "abcdefghij12345678901",
            "password",
            "12345678",
            "!@#$%^&*",
            "password 1",
            "비밀번호123456"
    })
    @NullAndEmptySource
    void signupRejectsInvalidPassword(String password) {
        SignupRequest request = new SignupRequest("홍길동", "gildong", "gildong@example.com", password);

        assertPasswordViolation(validator.validate(request), "password");
    }

    @ParameterizedTest(name = "비밀번호 변경 시 새 비밀번호 거부: {0}")
    @ValueSource(strings = {
            "abc123!",
            "abcdefghij12345678901",
            "password",
            "12345678",
            "!@#$%^&*",
            "password 1",
            "비밀번호123456"
    })
    @NullAndEmptySource
    void changePasswordRejectsInvalidNewPassword(String password) {
        ChangePasswordRequest request = new ChangePasswordRequest("oldPassword123!", password);

        assertPasswordViolation(validator.validate(request), "newPassword");
    }

    private void assertPasswordViolation(Set<? extends ConstraintViolation<?>> violations, String property) {
        assertThat(violations)
                .singleElement()
                .satisfies(violation -> {
                    assertThat(violation.getPropertyPath().toString()).isEqualTo(property);
                    assertThat(violation.getMessage()).isEqualTo(ERROR_MESSAGE);
                });
    }
}
