package com.promsearch.prompt.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.promsearch.prompt.application.port.out.prompt.LoadPromptEditPort;
import com.promsearch.prompt.application.port.out.prompt.LoadPromptEditPort.ImageProjection;
import com.promsearch.prompt.application.port.out.prompt.LoadPromptEditPort.PromptEditProjection;
import com.promsearch.prompt.application.port.out.storage.PresignPromptImageDownloadPort;
import com.promsearch.prompt.application.usecase.dto.PromptEditInfo;
import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.domain.enums.PromptVisibility;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PromptEditQueryServiceTest {

    @Mock
    private LoadPromptEditPort loadPromptEditPort;

    @Mock
    private PresignPromptImageDownloadPort imageStorage;

    private PromptEditQueryService service;

    @BeforeEach
    void setUp() {
        service = new PromptEditQueryService(loadPromptEditPort, imageStorage);
    }

    @DisplayName("작성자는 비공개 프롬프트의 수정 폼 데이터를 조회할 수 있다")
    @Test
    void getsOwnerEditData() {
        UUID imageId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        when(loadPromptEditPort.findById(10L)).thenReturn(Optional.of(new PromptEditProjection(
                10L, 1L, "제목", "설명", PromptOutputType.IMAGE,
                List.of(1L), List.of(2L), List.of(3L), null, PromptContentType.PREMIUM,
                "본문", PromptVisibility.PRIVATE,
                List.of(new ImageProjection(imageId, "watermarked/key", 0, true)),
                PromptStatus.ACTIVE, 500L, Instant.parse("2026-08-05T01:00:00Z")
        )));
        when(imageStorage.presignGet("watermarked/key")).thenReturn("https://image.example.com/signed");

        PromptEditInfo result = service.get(10L, 1L);

        assertThat(result.visibility()).isEqualTo(PromptVisibility.PRIVATE);
        assertThat(result.jobTagIds()).containsExactly(1L);
        assertThat(result.images()).singleElement()
                .extracting(PromptEditInfo.ImageInfo::imageUrl)
                .isEqualTo("https://image.example.com/signed");
    }

    @DisplayName("다른 작성자의 프롬프트 수정 조회는 거절한다")
    @Test
    void rejectsNonOwner() {
        when(loadPromptEditPort.findById(10L)).thenReturn(Optional.of(new PromptEditProjection(
                10L, 2L, "제목", null, PromptOutputType.TEXT,
                List.of(), List.of(), List.of(), "직접 입력", PromptContentType.FREE,
                "본문", PromptVisibility.PUBLIC, List.of(), PromptStatus.ACTIVE, 0L, Instant.now()
        )));

        assertThatThrownBy(() -> service.get(10L, 1L))
                .isInstanceOf(PromptDomainException.class)
                .extracting("baseCode")
                .isEqualTo(PromptErrorCode.PROMPT_NOT_OWNED);
    }
}
