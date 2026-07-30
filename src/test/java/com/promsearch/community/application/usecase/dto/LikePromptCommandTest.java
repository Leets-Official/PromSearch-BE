package com.promsearch.community.application.usecase.dto;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.promsearch.community.domain.exception.CommunityDomainException;
import com.promsearch.community.domain.exception.CommunityErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LikePromptCommandTest {

    @DisplayName("사용자 식별자는 양수여야 한다")
    @Test
    void userIdMustBePositive() {
        assertThatThrownBy(() -> new LikePromptCommand(0L, 1L))
                .isInstanceOf(CommunityDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CommunityErrorCode.INVALID_INTERACTION_USER_ID);
    }

    @DisplayName("프롬프트 식별자는 양수여야 한다")
    @Test
    void promptIdMustBePositive() {
        assertThatThrownBy(() -> new LikePromptCommand(1L, -1L))
                .isInstanceOf(CommunityDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CommunityErrorCode.INVALID_INTERACTION_POST_ID);
    }
}
