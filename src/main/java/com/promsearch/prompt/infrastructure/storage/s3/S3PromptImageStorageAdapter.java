package com.promsearch.prompt.infrastructure.storage.s3;

import com.promsearch.common.infrastructure.storage.ObjectStorageException;
import com.promsearch.common.infrastructure.storage.ObjectStorageOperations;
import com.promsearch.prompt.application.port.out.storage.DeletePromptImageObjectPort;
import com.promsearch.prompt.application.port.out.storage.LoadPromptImageObjectMetadataPort;
import com.promsearch.prompt.application.port.out.storage.PresignPromptImageDownloadPort;
import com.promsearch.prompt.application.port.out.storage.PresignPromptImageUploadPort;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 프롬프트 도메인 저장소 포트를 공통 Object Storage 동작에 연결하는 Adapter.
 */
@Component
@RequiredArgsConstructor
public class S3PromptImageStorageAdapter implements
        PresignPromptImageUploadPort,
        PresignPromptImageDownloadPort,
        LoadPromptImageObjectMetadataPort,
        DeletePromptImageObjectPort {

    private final ObjectStorageOperations objectStorage;

    @Override
    public PresignedUpload presignPut(String objectKey, String contentType) {
        try {
            ObjectStorageOperations.PresignedUpload upload =
                    objectStorage.presignPut(objectKey, contentType);
            return new PresignedUpload(upload.uploadUrl(), upload.expiresAt());
        } catch (ObjectStorageException exception) {
            throw translate(exception);
        }
    }

    @Override
    public String presignGet(String objectKey) {
        try {
            return objectStorage.presignGet(objectKey);
        } catch (ObjectStorageException exception) {
            throw translate(exception);
        }
    }

    @Override
    public StoredObjectMetadata getMetadata(String objectKey) {
        try {
            ObjectStorageOperations.StoredObjectMetadata metadata =
                    objectStorage.getMetadata(objectKey);
            return new StoredObjectMetadata(
                    metadata.contentLength(),
                    metadata.contentType(),
                    metadata.etag(),
                    metadata.lastModified()
            );
        } catch (ObjectStorageException exception) {
            if (exception.getReason() == ObjectStorageException.Reason.NOT_FOUND) {
                throw new PromptDomainException(PromptErrorCode.IMAGE_UPLOAD_NOT_FOUND);
            }
            throw translate(exception);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            objectStorage.delete(objectKey);
        } catch (ObjectStorageException exception) {
            throw translate(exception);
        }
    }

    private PromptDomainException translate(ObjectStorageException exception) {
        return new PromptDomainException(
                PromptErrorCode.IMAGE_STORAGE_UNAVAILABLE,
                PromptErrorCode.IMAGE_STORAGE_UNAVAILABLE.getMessage(),
                exception
        );
    }
}
