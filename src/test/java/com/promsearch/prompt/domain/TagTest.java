package com.promsearch.prompt.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.promsearch.prompt.domain.enums.TagType;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TagTest {

    @DisplayName("직접 입력한 AI 모델명은 소문자 변환하고 모든 공백을 제거해 정규화한다")
    @Test
    void normalizeCustomAiModel() {
        Tag tag = Tag.createCustomAiModel(" GPT  4.1\tMini ");

        assertThat(tag.getTagType()).isEqualTo(TagType.AI_MODEL);
        assertThat(tag.getTagName()).isEqualTo("GPT  4.1\tMini");
        assertThat(tag.getNormalizedName()).isEqualTo("gpt4.1mini");
        assertThat(tag.isCustom()).isTrue();
    }

    @DisplayName("직접 입력 AI 모델명은 50자를 초과할 수 없다")
    @Test
    void rejectCustomAiModelLongerThanFiftyCharacters() {
        assertThatThrownBy(() -> Tag.createCustomAiModel("a".repeat(Tag.MAX_CUSTOM_AI_MODEL_LENGTH + 1)))
                .isInstanceOf(PromptDomainException.class)
                .extracting("baseCode")
                .isEqualTo(PromptErrorCode.INVALID_TAG_NAME);
    }
}
