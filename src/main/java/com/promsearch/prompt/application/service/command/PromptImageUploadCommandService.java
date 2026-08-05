package com.promsearch.prompt.application.service.command;

import com.promsearch.prompt.application.port.out.promptimage.LoadPromptImagePort;
import com.promsearch.prompt.application.port.out.promptimage.SavePromptImagePort;
import com.promsearch.prompt.application.port.out.promptimage.SavePromptImageWatermarkJobPort;
import com.promsearch.prompt.application.port.out.storage.DeletePromptImageObjectPort;
import com.promsearch.prompt.application.port.out.storage.GeneratePromptImageObjectKeyPort;
import com.promsearch.prompt.application.port.out.storage.LoadPromptImageObjectMetadataPort;
import com.promsearch.prompt.application.port.out.storage.LoadPromptImageObjectMetadataPort.StoredObjectMetadata;
import com.promsearch.prompt.application.port.out.storage.PresignPromptImageUploadPort;
import com.promsearch.prompt.application.port.out.storage.PresignPromptImageUploadPort.PresignedUpload;
import com.promsearch.prompt.application.usecase.CompletePromptImageUploadUseCase;
import com.promsearch.prompt.application.usecase.IssuePromptImageUploadUrlsUseCase;
import com.promsearch.prompt.application.usecase.dto.CompletePromptImageUploadCommand;
import com.promsearch.prompt.application.usecase.dto.IssuePromptImageUploadUrlsCommand;
import com.promsearch.prompt.application.usecase.dto.PromptImageUploadInfo;
import com.promsearch.prompt.application.usecase.dto.PromptImageUploadUrlsInfo;
import com.promsearch.prompt.application.usecase.dto.PromptImageUploadUrlsInfo.UploadTarget;
import com.promsearch.prompt.application.usecase.dto.PromptImageWatermarkJob;
import com.promsearch.prompt.domain.PromptImage;
import com.promsearch.prompt.domain.enums.PromptImageContentType;
import com.promsearch.prompt.domain.enums.PromptImageStatus;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이미지 업로드 준비·완료 검증 흐름 조율
 *
 * <p>프론트엔드와 S3 사이의 파일 전송과 분리된 이미지 상태·객체 메타데이터 관리</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PromptImageUploadCommandService implements
        IssuePromptImageUploadUrlsUseCase,
        CompletePromptImageUploadUseCase {

    private static final int MAX_IMAGE_COUNT = 10;
    private static final int CURRENT_WATERMARK_PROCESSING_VERSION = 1;

    private final LoadPromptImagePort loadPromptImagePort;
    private final SavePromptImagePort savePromptImagePort;
    private final GeneratePromptImageObjectKeyPort generatePromptImageObjectKeyPort;
    private final PresignPromptImageUploadPort presignPromptImageUploadPort;
    private final LoadPromptImageObjectMetadataPort loadPromptImageObjectMetadataPort;
    private final DeletePromptImageObjectPort deletePromptImageObjectPort;
    private final SavePromptImageWatermarkJobPort savePromptImageWatermarkJobPort;

    /**
     * 이미지 자산 준비 → Object Key 생성 → Presigned PUT URL 발급 → 이미지 일괄 저장
     */
    @Override
    public PromptImageUploadUrlsInfo issue(IssuePromptImageUploadUrlsCommand command) {
        validateIssueCommand(command);

        List<PromptImage> images = new ArrayList<>(command.images().size());
        List<UploadTarget> uploadTargets = new ArrayList<>(command.images().size());

        for (IssuePromptImageUploadUrlsCommand.ImageFile file : command.images()) {
            PromptImageContentType contentType = PromptImageContentType.fromMimeType(file.contentType());
            UUID imageId = UUID.randomUUID();
            String objectKey = generatePromptImageObjectKeyPort.generateOriginal(
                    command.uploaderId(),
                    imageId,
                    contentType
            );

            PromptImage image = PromptImage.prepareUpload(
                    imageId,
                    command.uploaderId(),
                    objectKey,
                    file.fileName(),
                    contentType,
                    file.fileSize(),
                    file.width(),
                    file.height()
            );
            PresignedUpload presignedUpload = presignPromptImageUploadPort.presignPut(
                    objectKey,
                    contentType.getMimeType(),
                    file.fileSize()
            );

            images.add(image);
            uploadTargets.add(new UploadTarget(
                    imageId,
                    presignedUpload.uploadUrl(),
                    presignedUpload.expiresAt()
            ));
        }

        savePromptImagePort.createAll(images);
        // TODO: 장기 UPLOADING 레코드와 고아 S3 객체를 정리하는 배치·Lifecycle 정책 추가
        log.info("prompt_image_upload_urls_issued uploaderId={} imageCount={}",
                command.uploaderId(), images.size());
        return new PromptImageUploadUrlsInfo(List.copyOf(uploadTargets));
    }

    /**
     * 이미지·소유권 조회 → S3 메타데이터 검증 → UPLOADED 상태 전환
     */
    @Override
    public PromptImageUploadInfo complete(CompletePromptImageUploadCommand command) {
        PromptImage image = loadPromptImagePort.getById(command.imageId());
        if (!image.isOwnedBy(command.uploaderId())) {
            throw new PromptDomainException(PromptErrorCode.IMAGE_NOT_OWNED);
        }

        // 완료 요청 재시도 시 S3를 다시 조회하거나 상태를 되돌리지 않는다.
        if (image.isUploadCompleted()) {
            return PromptImageUploadInfo.from(image);
        }
        if (image.getStatus() != PromptImageStatus.UPLOADING) {
            throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_STATUS_TRANSITION);
        }

        // TODO: S3 조회를 트랜잭션 밖으로 분리하고 재조회·낙관적 잠금 기반 동시 완료 테스트 추가
        StoredObjectMetadata metadata =
                loadPromptImageObjectMetadataPort.getMetadata(image.getOriginalObjectKey());
        if (!matchesExpectedMetadata(image, metadata)) {
            // TODO: 임시 prefix 또는 객체 태그 기반 Lifecycle 도입 후 삭제 실패를 보완하는 정리 정책 추가
            deletePromptImageObjectPort.delete(image.getOriginalObjectKey());
            throw new PromptDomainException(PromptErrorCode.IMAGE_UPLOAD_METADATA_MISMATCH);
        }

        image.completeUpload(metadata.etag(), metadata.lastModified());
        PromptImage updatedImage = savePromptImagePort.update(image);
        savePromptImageWatermarkJobPort.save(createWatermarkJob(updatedImage));
        log.info("prompt_image_upload_completed uploaderId={} imageId={}",
                command.uploaderId(), command.imageId());
        return PromptImageUploadInfo.from(updatedImage);
    }

    /** 업로드 완료 이미지의 원본·결과 Key와 처리 버전을 담은 Outbox 작업 생성 */
    private PromptImageWatermarkJob createWatermarkJob(PromptImage image) {
        return new PromptImageWatermarkJob(
                PromptImageWatermarkJob.CURRENT_EVENT_VERSION,
                UUID.randomUUID(),
                image.getPromptImageId().id(),
                image.getOriginalObjectKey(),
                generatePromptImageObjectKeyPort.generateWatermarked(
                        image.getUploaderId(),
                        image.getPromptImageId().id(),
                        image.getContentType()
                ),
                image.getContentType().getMimeType(),
                CURRENT_WATERMARK_PROCESSING_VERSION,
                Instant.now()
        );
    }

    /** S3 HeadObject 결과가 URL 발급 당시 선언한 이미지 메타데이터와 일치하는지 확인 */
    private boolean matchesExpectedMetadata(PromptImage image, StoredObjectMetadata metadata) {
        return metadata != null
                && metadata.contentLength() == image.getFileSize()
                && metadata.contentType() != null
                && metadata.etag() != null
                && !metadata.etag().isBlank()
                && metadata.lastModified() != null
                && metadata.contentType().trim().toLowerCase(Locale.ROOT)
                .equals(image.getContentType().getMimeType());
    }

    /** HTTP 검증을 우회한 호출에서도 업로더와 이미지 개수 정책을 보장 */
    private void validateIssueCommand(IssuePromptImageUploadUrlsCommand command) {
        if (command == null
                || command.images() == null
                || command.images().isEmpty()
                || command.images().size() > MAX_IMAGE_COUNT
                || command.images().stream().anyMatch(java.util.Objects::isNull)) {
            throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_UPLOAD_COUNT);
        }
        if (command.uploaderId() == null || command.uploaderId() <= 0) {
            throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_UPLOADER_ID);
        }
    }
}
