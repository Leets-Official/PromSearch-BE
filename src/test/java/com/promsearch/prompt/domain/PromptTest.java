package com.promsearch.prompt.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.domain.enums.PromptVisibility;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PromptTest {

    @DisplayName("게시 프롬프트는 ACTIVE 상태와 공개 범위로 생성된다")
    @Test
    void createActivePrompt() {
        Prompt prompt = Prompt.createActive(
                1L,
                " 프롬프트 제목 ",
                "프롬프트 본문",
                PromptOutputType.TEXT,
                "설명",
                PromptContentType.FREE,
                PromptVisibility.PRIVATE,
                0L
        );

        assertThat(prompt.getTitle()).isEqualTo("프롬프트 제목");
        assertThat(prompt.getStatus()).isEqualTo(PromptStatus.ACTIVE);
        assertThat(prompt.getVisibility()).isEqualTo(PromptVisibility.PRIVATE);
        assertThat(prompt.getPricePoint()).isZero();
    }

    @DisplayName("FREE는 0포인트, PREMIUM은 양수 가격만 허용한다")
    @Test
    void validateServerPricePolicy() {
        assertPromptError(
                () -> create(PromptContentType.FREE, 100L),
                PromptErrorCode.INVALID_PRICE_POINT
        );
        assertPromptError(
                () -> create(PromptContentType.PREMIUM, 0L),
                PromptErrorCode.INVALID_PRICE_POINT
        );
        assertThat(create(PromptContentType.PREMIUM, 100L).getPricePoint()).isEqualTo(100L);
    }

    @DisplayName("게시 프롬프트는 설명, 본문과 20자 이하 제목이 필요하다")
    @Test
    void validatePublishedContent() {
        assertPromptError(
                () -> Prompt.createActive(
                        1L,
                        "제목",
                        " ",
                        PromptOutputType.TEXT,
                        "설명",
                        PromptContentType.FREE,
                        PromptVisibility.PUBLIC,
                        0L
                ),
                PromptErrorCode.INVALID_PROMPT_BODY
        );
        assertPromptError(
                () -> Prompt.createActive(
                        1L,
                        "123456789012345678901",
                        "본문",
                        PromptOutputType.TEXT,
                        "설명",
                        PromptContentType.FREE,
                        PromptVisibility.PUBLIC,
                        0L
                ),
                PromptErrorCode.INVALID_PROMPT_TITLE
        );
        assertPromptError(
                () -> Prompt.createActive(
                        1L,
                        "제목",
                        "본문",
                        PromptOutputType.TEXT,
                        " ",
                        PromptContentType.FREE,
                        PromptVisibility.PUBLIC,
                        0L
                ),
                PromptErrorCode.INVALID_PROMPT_DESCRIPTION
        );
    }

    private Prompt create(PromptContentType contentType, Long pricePoint) {
        return Prompt.createActive(
                1L,
                "제목",
                "본문",
                PromptOutputType.TEXT,
                "설명",
                contentType,
                PromptVisibility.PUBLIC,
                pricePoint
        );
    }

    private void assertPromptError(Runnable action, PromptErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(PromptDomainException.class)
                .extracting("baseCode")
                .isEqualTo(errorCode);
    }
}
