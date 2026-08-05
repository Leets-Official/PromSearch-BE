package com.promsearch.prompt.application.service.command;

import com.promsearch.prompt.application.port.out.storage.LoadPromptImageBinaryPort;
import com.promsearch.prompt.application.port.out.storage.LoadPromptImageBinaryPort.StoredImage;
import com.promsearch.prompt.application.port.out.storage.RenderPromptImageWatermarkPort;
import com.promsearch.prompt.application.port.out.storage.RenderPromptImageWatermarkPort.RenderedImage;
import com.promsearch.prompt.application.port.out.storage.SavePromptImageBinaryPort;
import com.promsearch.prompt.application.service.command.PromptImageProcessingStateService.ProcessingContext;
import com.promsearch.prompt.application.usecase.ProcessPromptImageWatermarkUseCase;
import com.promsearch.prompt.application.usecase.dto.PromptImageWatermarkJob;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 네트워크·CPU 작업을 트랜잭션 밖에서 조율하는 워터마크 처리기 */
@Slf4j
@RequiredArgsConstructor
public class PromptImageWatermarkProcessor implements ProcessPromptImageWatermarkUseCase {

    private final PromptImageProcessingStateService stateService;
    private final LoadPromptImageBinaryPort loadPromptImageBinaryPort;
    private final RenderPromptImageWatermarkPort renderPromptImageWatermarkPort;
    private final SavePromptImageBinaryPort savePromptImageBinaryPort;

    /** 상태 시작 → 원본 다운로드 → 합성 → 결과 업로드 → READY 상태 변경 */
    @Override
    public void process(PromptImageWatermarkJob job) {
        ProcessingContext context = stateService.begin(job);
        if (!context.processingRequired()) {
            log.info("prompt_image_watermark_skipped imageId={} processingVersion={}",
                    job.imageId(), job.processingVersion());
            return;
        }

        try {
            StoredImage source = loadPromptImageBinaryPort.load(job.originalObjectKey());
            byte[] sourceBytes = source.bytes();
            validateSource(job, context, sourceBytes.length, source.contentType());
            RenderedImage rendered = renderPromptImageWatermarkPort.render(
                    sourceBytes,
                    source.contentType(),
                    context.expectedWidth(),
                    context.expectedHeight()
            );
            savePromptImageBinaryPort.save(
                    job.watermarkedObjectKey(),
                    rendered.contentType(),
                    rendered.bytes()
            );
            stateService.complete(job);
            log.info("prompt_image_watermark_completed imageId={} processingVersion={}",
                    job.imageId(), job.processingVersion());
        } catch (RuntimeException exception) {
            PromptErrorCode failureCode = failureCode(exception);
            recordFailure(job, failureCode, exception);
            throw exception;
        }
    }

    /** 다운로드한 S3 바이너리의 크기와 형식이 작업 메시지·DB 정보와 일치하는지 확인 */
    private void validateSource(
            PromptImageWatermarkJob job,
            ProcessingContext context,
            int sourceSize,
            String sourceContentType
    ) {
        if (sourceSize != context.expectedFileSize()
                || !sourceContentType.equalsIgnoreCase(job.contentType())) {
            throw new PromptDomainException(
                    PromptErrorCode.INVALID_IMAGE_SOURCE,
                    "S3 원본 이미지 메타데이터가 이미지 자산과 일치하지 않습니다."
            );
        }
    }

    /** 알려진 처리 오류는 유지하고 예상하지 못한 오류는 공통 처리 실패로 정규화 */
    private PromptErrorCode failureCode(RuntimeException exception) {
        if (exception instanceof PromptDomainException domainException
                && domainException.getBaseCode() instanceof PromptErrorCode promptErrorCode
                && isProcessingFailure(promptErrorCode)) {
            return promptErrorCode;
        }
        return PromptErrorCode.IMAGE_PROCESSING_FAILED;
    }

    /** 이미지 상태에 기록해 Worker 재시도 판단에 사용할 수 있는 실패 코드인지 확인 */
    private boolean isProcessingFailure(PromptErrorCode errorCode) {
        return switch (errorCode) {
            case IMAGE_ORIGINAL_DOWNLOAD_FAILED,
                    INVALID_IMAGE_SOURCE,
                    IMAGE_WATERMARK_RENDER_FAILED,
                    IMAGE_WATERMARK_UPLOAD_FAILED,
                    IMAGE_PROCESSING_FAILED -> true;
            default -> false;
        };
    }

    /** 원래 처리 예외를 보존하면서 FAILED 상태 기록 실패는 suppressed 예외로 첨부 */
    private void recordFailure(
            PromptImageWatermarkJob job,
            PromptErrorCode failureCode,
            RuntimeException originalException
    ) {
        try {
            stateService.fail(job, failureCode);
        } catch (RuntimeException stateException) {
            originalException.addSuppressed(stateException);
            log.error("prompt_image_watermark_failure_record_failed imageId={} failureCode={}",
                    job.imageId(), failureCode, stateException);
        }
    }
}
