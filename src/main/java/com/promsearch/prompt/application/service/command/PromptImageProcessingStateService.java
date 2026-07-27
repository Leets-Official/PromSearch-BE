package com.promsearch.prompt.application.service.command;

import com.promsearch.prompt.application.port.out.promptimage.LoadPromptImagePort;
import com.promsearch.prompt.application.port.out.promptimage.SavePromptImagePort;
import com.promsearch.prompt.application.usecase.dto.PromptImageWatermarkJob;
import com.promsearch.prompt.domain.PromptImage;
import com.promsearch.prompt.domain.enums.PromptImageStatus;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/** S3·CPU 작업과 분리된 짧은 이미지 상태 변경 트랜잭션 */
@RequiredArgsConstructor
public class PromptImageProcessingStateService {

    private final LoadPromptImagePort loadPromptImagePort;
    private final SavePromptImagePort savePromptImagePort;

    /**
     * 메시지와 이미지 자산 일치 여부를 검증하고 PROCESSING 상태로 전환
     *
     * <p>PROCESSING 재수신은 이전 Worker 장애 복구를 위해 다시 처리하고, 동일한 READY 작업은 건너뛴다.</p>
     */
    @Transactional
    public ProcessingContext begin(PromptImageWatermarkJob job) {
        PromptImage image = loadAndValidate(job);

        if (isCompletedBy(image, job) || isOlderThanCurrentResult(image, job)) {
            return ProcessingContext.skip();
        }
        if (image.getStatus() == PromptImageStatus.UPLOADED
                || image.getStatus() == PromptImageStatus.FAILED) {
            image.startProcessing();
            savePromptImagePort.update(image);
        } else if (image.getStatus() != PromptImageStatus.PROCESSING) {
            throw invalidState(image);
        }

        return ProcessingContext.process(
                image.getFileSize(),
                image.getWidth(),
                image.getHeight()
        );
    }

    /** 결과 Object Key와 처리 버전을 기록하고 READY 상태로 전환 */
    @Transactional
    public void complete(PromptImageWatermarkJob job) {
        PromptImage image = loadAndValidate(job);
        if (isCompletedBy(image, job) || isOlderThanCurrentResult(image, job)) {
            return;
        }
        if (image.getStatus() != PromptImageStatus.PROCESSING) {
            throw invalidState(image);
        }

        image.completeProcessing(job.watermarkedObjectKey(), job.processingVersion());
        savePromptImagePort.update(image);
    }

    /** 처리 중인 이미지에 재시도 가능한 실패 코드를 기록 */
    @Transactional
    public void fail(PromptImageWatermarkJob job, PromptErrorCode failureCode) {
        PromptImage image = loadPromptImagePort.getById(job.imageId());
        if (image.getStatus() != PromptImageStatus.PROCESSING) {
            return;
        }

        image.failProcessing(failureCode.getCode());
        savePromptImagePort.update(image);
    }

    private PromptImage loadAndValidate(PromptImageWatermarkJob job) {
        if (job == null) {
            throw new PromptDomainException(
                    PromptErrorCode.INVALID_IMAGE_WATERMARK_JOB,
                    "워터마크 작업 메시지는 필수입니다."
            );
        }

        PromptImage image = loadPromptImagePort.getById(job.imageId());
        if (!image.getOriginalObjectKey().equals(job.originalObjectKey())
                || !image.getContentType().getMimeType().equals(job.contentType())) {
            throw new PromptDomainException(
                    PromptErrorCode.INVALID_IMAGE_WATERMARK_JOB,
                    "워터마크 작업 메시지가 이미지 자산과 일치하지 않습니다."
            );
        }
        return image;
    }

    private boolean isCompletedBy(PromptImage image, PromptImageWatermarkJob job) {
        return image.getStatus() == PromptImageStatus.READY
                && image.getProcessingVersion() == job.processingVersion()
                && job.watermarkedObjectKey().equals(image.getWatermarkedObjectKey());
    }

    private boolean isOlderThanCurrentResult(PromptImage image, PromptImageWatermarkJob job) {
        return image.getStatus() == PromptImageStatus.READY
                && image.getProcessingVersion() > job.processingVersion();
    }

    private PromptDomainException invalidState(PromptImage image) {
        return new PromptDomainException(
                PromptErrorCode.INVALID_IMAGE_STATUS_TRANSITION,
                "워터마크 처리 불가 상태입니다: " + image.getStatus()
        );
    }

    public record ProcessingContext(
            boolean processingRequired,
            long expectedFileSize,
            int expectedWidth,
            int expectedHeight
    ) {

        private static ProcessingContext skip() {
            return new ProcessingContext(false, 0, 0, 0);
        }

        private static ProcessingContext process(long fileSize, int width, int height) {
            return new ProcessingContext(true, fileSize, width, height);
        }
    }
}
