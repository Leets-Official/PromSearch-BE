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
                List.of(1L, 2L, 3L),
                List.of(4L, 5L, 6L)
        )).doesNotThrowAnyException();
    }

    @Test
    void rejectsMoreThanThreeTagsPerType() {
        assertThatThrownBy(() -> InterestTagSelectionPolicy.validate(
                List.of(1L, 2L, 3L, 4L),
                List.of()
        )).isInstanceOf(UserDomainException.class);
    }

    @Test
    void rejectsDuplicateTags() {
        assertThatThrownBy(() -> InterestTagSelectionPolicy.validate(
                List.of(1L, 1L),
                List.of()
        )).isInstanceOf(UserDomainException.class);
    }
}
