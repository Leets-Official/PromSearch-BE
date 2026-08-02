package com.promsearch.prompt.application.service.command;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.promsearch.prompt.application.port.out.storage.LoadPromptImageBinaryPort;
import com.promsearch.prompt.application.port.out.storage.LoadPromptImageBinaryPort.StoredImage;
import com.promsearch.prompt.application.port.out.storage.RenderPromptImageWatermarkPort;
import com.promsearch.prompt.application.port.out.storage.RenderPromptImageWatermarkPort.RenderedImage;
import com.promsearch.prompt.application.port.out.storage.SavePromptImageBinaryPort;
import com.promsearch.prompt.application.service.command.PromptImageProcessingStateService.ProcessingContext;
import com.promsearch.prompt.application.usecase.dto.PromptImageWatermarkJob;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PromptImageWatermarkProcessorTest {

    @Mock
    private PromptImageProcessingStateService stateService;
    @Mock
    private LoadPromptImageBinaryPort loadPromptImageBinaryPort;
    @Mock
    private RenderPromptImageWatermarkPort renderPromptImageWatermarkPort;
    @Mock
    private SavePromptImageBinaryPort savePromptImageBinaryPort;

    private PromptImageWatermarkProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new PromptImageWatermarkProcessor(
                stateService,
                loadPromptImageBinaryPort,
                renderPromptImageWatermarkPort,
                savePromptImageBinaryPort
        );
    }

    @DisplayName("원본 다운로드부터 결과 업로드와 READY 상태 변경까지 조율한다")
    @Test
    void processWatermarkJob() {
        PromptImageWatermarkJob job = job();
        byte[] source = {1, 2, 3};
        byte[] result = {4, 5, 6};
        when(stateService.begin(job))
                .thenReturn(new ProcessingContext(true, 3, 640, 360));
        when(loadPromptImageBinaryPort.load(job.originalObjectKey()))
                .thenReturn(new StoredImage(source, "image/png"));
        when(renderPromptImageWatermarkPort.render(source, "image/png", 640, 360))
                .thenReturn(new RenderedImage(result, "image/png", 640, 360));

        processor.process(job);

        verify(savePromptImageBinaryPort)
                .save(job.watermarkedObjectKey(), "image/png", result);
        verify(stateService).complete(job);
    }

    @DisplayName("이미 완료된 메시지는 S3와 렌더러를 호출하지 않는다")
    @Test
    void skipCompletedJob() {
        PromptImageWatermarkJob job = job();
        when(stateService.begin(job))
                .thenReturn(new ProcessingContext(false, 0, 0, 0));

        processor.process(job);

        verify(loadPromptImageBinaryPort, never()).load(job.originalObjectKey());
        verify(renderPromptImageWatermarkPort, never())
                .render(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
        verify(savePromptImageBinaryPort, never())
                .save(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
    }

    @DisplayName("렌더링 실패를 이미지 FAILED 상태로 기록하고 예외를 다시 던진다")
    @Test
    void recordRenderingFailure() {
        PromptImageWatermarkJob job = job();
        byte[] source = {1, 2, 3};
        PromptDomainException failure = new PromptDomainException(
                PromptErrorCode.IMAGE_WATERMARK_RENDER_FAILED,
                "렌더링 실패"
        );
        when(stateService.begin(job))
                .thenReturn(new ProcessingContext(true, 3, 640, 360));
        when(loadPromptImageBinaryPort.load(job.originalObjectKey()))
                .thenReturn(new StoredImage(source, "image/png"));
        when(renderPromptImageWatermarkPort.render(source, "image/png", 640, 360))
                .thenThrow(failure);

        assertThatThrownBy(() -> processor.process(job)).isSameAs(failure);

        verify(stateService).fail(
                job,
                PromptErrorCode.IMAGE_WATERMARK_RENDER_FAILED
        );
        verify(savePromptImageBinaryPort, never())
                .save(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
    }

    private PromptImageWatermarkJob job() {
        UUID imageId = UUID.randomUUID();
        return new PromptImageWatermarkJob(
                PromptImageWatermarkJob.CURRENT_EVENT_VERSION,
                UUID.randomUUID(),
                imageId,
                "prompt-images/original/1/" + imageId + ".png",
                "prompt-images/watermarked/1/" + imageId + ".png",
                "image/png",
                1,
                Instant.parse("2026-07-27T00:00:00Z")
        );
    }
}
