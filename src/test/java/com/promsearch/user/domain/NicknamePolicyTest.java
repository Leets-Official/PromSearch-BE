package com.promsearch.user.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.promsearch.user.domain.exception.UserDomainException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NicknamePolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {"개발자1", "Prompt123", "한글English1", "1234567890"})
    void acceptsKoreanEnglishAndNumbersUpToTenCharacters(String nickname) {
        assertThatCode(() -> NicknamePolicy.validate(nickname)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "개발자 닉네임", "prompt-master", "12345678901", "nickname!"})
    void rejectsInvalidNickname(String nickname) {
        assertThatThrownBy(() -> NicknamePolicy.validate(nickname))
                .isInstanceOf(UserDomainException.class);
    }
}
