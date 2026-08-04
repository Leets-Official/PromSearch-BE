package com.promsearch.user.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.promsearch.user.domain.exception.UserDomainException;
import java.util.List;
import org.junit.jupiter.api.Test;

class InterestTagSelectionPolicyTest {

    @Test
    void acceptsUpToThreeTagsPerType() {
        assertThatCode(() -> InterestTagSelectionPolicy.validate(
                List.of("학생", "직장인", "개발자"),
                List.of("PPT", "보고서", "이미지 생성")
        )).doesNotThrowAnyException();
    }

    @Test
    void rejectsMoreThanThreeTagsPerType() {
        assertThatThrownBy(() -> InterestTagSelectionPolicy.validate(
                List.of("학생", "직장인", "자영업자", "개발자"),
                List.of()
        )).isInstanceOf(UserDomainException.class);
    }

    @Test
    void rejectsDuplicateTags() {
        assertThatThrownBy(() -> InterestTagSelectionPolicy.validate(
                List.of("개발자", "개발자"),
                List.of()
        )).isInstanceOf(UserDomainException.class);
    }
}
