package com.promsearch.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.promsearch.auth.application.usecase.dto.LoginCommand;
import com.promsearch.auth.interfaces.dto.request.LoginRequest;
import com.promsearch.auth.interfaces.dto.request.SignupRequest;
import com.promsearch.user.application.usecase.dto.ChangePasswordCommand;
import com.promsearch.user.application.usecase.dto.SignupCommand;
import com.promsearch.user.interfaces.dto.request.ChangePasswordRequest;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CredentialLoggingSafetyTest {

    private static final String CURRENT_PASSWORD = "current123!";
    private static final String NEW_PASSWORD = "newPassword123!";

    @DisplayName("자격증명 객체의 문자열 표현에는 평문 비밀번호가 노출되지 않는다")
    @ParameterizedTest
    @MethodSource("credentialsContainingPasswords")
    void masksRawPasswordsInStringRepresentation(Object credential) {
        assertThat(credential.toString())
                .contains("***")
                .doesNotContain(CURRENT_PASSWORD, NEW_PASSWORD);
    }

    private static Stream<Arguments> credentialsContainingPasswords() {
        return Stream.of(
                Arguments.of(LoginCommand.of("user@example.com", CURRENT_PASSWORD)),
                Arguments.of(SignupCommand.of("name", "nickname", "user@example.com", CURRENT_PASSWORD)),
                Arguments.of(ChangePasswordCommand.of(1L, CURRENT_PASSWORD, NEW_PASSWORD)),
                Arguments.of(new LoginRequest("user@example.com", CURRENT_PASSWORD)),
                Arguments.of(new SignupRequest("name", "nickname", "user@example.com", CURRENT_PASSWORD)),
                Arguments.of(new ChangePasswordRequest(CURRENT_PASSWORD, NEW_PASSWORD))
        );
    }
}
