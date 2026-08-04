package com.promsearch.prompt.application.service.query;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.promsearch.prompt.application.port.out.prompt.LoadPromptDraftPort;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PromptDraftQueryServiceTest {

    @Mock
    private LoadPromptDraftPort loadPromptDraftPort;

    private PromptDraftQueryService service;

    @BeforeEach
    void setUp() {
        service = new PromptDraftQueryService(loadPromptDraftPort);
    }

    @DisplayName("임시저장이 없으면 조회를 실패한다")
    @Test
    void getDraftFailsWhenMissing() {
        when(loadPromptDraftPort.findDraftByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(1L))
                .isInstanceOf(PromptDomainException.class)
                .extracting("baseCode")
                .isEqualTo(PromptErrorCode.PROMPT_NOT_FOUND);
    }
}
