package com.promsearch.prompt.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.promsearch.prompt.application.port.out.promptimage.LoadPromptImagePort;
import com.promsearch.prompt.application.port.out.promptimage.SavePromptImagePort;
import com.promsearch.prompt.application.usecase.dto.PromptImageWatermarkJob;
import com.promsearch.prompt.domain.PromptImage;
import com.promsearch.prompt.domain.enums.PromptImageContentType;
import com.promsearch.prompt.domain.enums.PromptImageStatus;
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
class PromptImageProcessingStateServiceTest {

    @Mock
    private LoadPromptImagePort loadPromptImagePort;
    @Mock
    private SavePromptImagePort savePromptImagePort;

    private PromptImageProcessingStateService service;

    @BeforeEach
    void setUp() {
        service = new PromptImageProcessingStateService(
                loadPromptImagePort,
                savePromptImagePort
        );
    }

    @DisplayName("UPLOADED 이미지를 PROCESSING으로 바꾸고 검증할 메타데이터를 반환한다")
    @Test
    void beginProcessing() {
        PromptImage image = uploadedImage();
        PromptImageWatermarkJob job = job(image);
        when(loadPromptImagePort.getById(job.imageId())).thenReturn(image);

        PromptImageProcessingStateService.ProcessingContext context = service.begin(job);

        assertThat(context.processingRequired()).isTrue();
        assertThat(context.expectedFileSize()).isEqualTo(image.getFileSize());
        assertThat(context.expectedWidth()).isEqualTo(image.getWidth());
        assertThat(context.expectedHeight()).isEqualTo(image.getHeight());
        assertThat(image.getStatus()).isEqualTo(PromptImageStatus.PROCESSING);
        verify(savePromptImagePort).update(image);
    }

    @DisplayName("동일한 READY 작업을 다시 받으면 이미지 처리와 상태 저장을 건너뛴다")
    @Test
    void skipCompletedJob() {
        PromptImage image = uploadedImage();
        PromptImageWatermarkJob job = job(image);
        image.startProcessing();
        image.completeProcessing(job.watermarkedObjectKey(), job.processingVersion());
        when(loadPromptImagePort.getById(job.imageId())).thenReturn(image);

        PromptImageProcessingStateService.ProcessingContext context = service.begin(job);

        assertThat(context.processingRequired()).isFalse();
        verify(savePromptImagePort, never()).update(image);
    }

    @DisplayName("처리 결과를 기록하고 PROCESSING 이미지를 READY로 바꾼다")
    @Test
    void completeProcessing() {
        PromptImage image = uploadedImage();
        PromptImageWatermarkJob job = job(image);
        image.startProcessing();
        when(loadPromptImagePort.getById(job.imageId())).thenReturn(image);

        service.complete(job);

        assertThat(image.getStatus()).isEqualTo(PromptImageStatus.READY);
        assertThat(image.getWatermarkedObjectKey()).isEqualTo(job.watermarkedObjectKey());
        assertThat(image.getProcessingVersion()).isEqualTo(job.processingVersion());
        verify(savePromptImagePort).update(image);
    }

    @DisplayName("Worker 오류 코드를 기록하고 PROCESSING 이미지를 FAILED로 바꾼다")
    @Test
    void failProcessing() {
        PromptImage image = uploadedImage();
        PromptImageWatermarkJob job = job(image);
        image.startProcessing();
        when(loadPromptImagePort.getById(job.imageId())).thenReturn(image);

        service.fail(job, PromptErrorCode.IMAGE_WATERMARK_RENDER_FAILED);

        assertThat(image.getStatus()).isEqualTo(PromptImageStatus.FAILED);
        assertThat(image.getFailureCode()).isEqualTo("PROMPT-044");
        verify(savePromptImagePort).update(image);
    }

    private PromptImage uploadedImage() {
        UUID imageId = UUID.randomUUID();
        PromptImage image = PromptImage.prepareUpload(
                imageId,
                1L,
                "prompt-images/original/1/" + imageId + ".png",
                "result.png",
                PromptImageContentType.PNG,
                1_024,
                640,
                360
        );
        image.completeUpload("\"etag\"", Instant.parse("2026-07-27T00:00:00Z"));
        return image;
    }

    private PromptImageWatermarkJob job(PromptImage image) {
        return new PromptImageWatermarkJob(
                PromptImageWatermarkJob.CURRENT_EVENT_VERSION,
                UUID.randomUUID(),
                image.getPromptImageId().id(),
                image.getOriginalObjectKey(),
                "prompt-images/watermarked/1/" + image.getPromptImageId().id() + ".png",
                image.getContentType().getMimeType(),
                1,
                Instant.parse("2026-07-27T00:01:00Z")
        );
    }
}
