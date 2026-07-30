package com.promsearch.common.infrastructure.storage.s3;

import com.promsearch.common.infrastructure.storage.ObjectStorageException;
import com.promsearch.common.infrastructure.storage.ObjectStorageOperations;
import java.net.URI;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * 공통 Object Storage 계약을 AWS S3 SDK로 구현하는 어댑터.
 *
 * <p>S3 예외를 공통 저장소 예외로 변환하여 사용하는 도메인이 AWS SDK 예외에
 * 직접 의존하지 않도록 한다.</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class S3ObjectStorageOperations implements ObjectStorageOperations {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3ObjectStorageProperties properties;

    @Override
    public PresignedUpload presignPut(String objectKey, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .contentType(contentType)
                .build();
        PutObjectPresignRequest request = PutObjectPresignRequest.builder()
                .signatureDuration(properties.uploadUrlExpiration())
                .putObjectRequest(putObjectRequest)
                .build();
        try {
            return new PresignedUpload(
                    URI.create(s3Presigner.presignPutObject(request).url().toString()),
                    Instant.now().plus(properties.uploadUrlExpiration())
            );
        } catch (SdkException exception) {
            throw unavailable("presignPut", objectKey, exception);
        }
    }

    @Override
    public String presignGet(String objectKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .build();
        GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                .signatureDuration(properties.uploadUrlExpiration())
                .getObjectRequest(getObjectRequest)
                .build();
        try {
            return s3Presigner.presignGetObject(request).url().toString();
        } catch (SdkException exception) {
            throw unavailable("presignGet", objectKey, exception);
        }
    }

    @Override
    public StoredObjectMetadata getMetadata(String objectKey) {
        HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .build();
        try {
            HeadObjectResponse response = s3Client.headObject(request);
            return new StoredObjectMetadata(
                    response.contentLength(),
                    response.contentType(),
                    response.eTag(),
                    response.lastModified()
            );
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                throw new ObjectStorageException(ObjectStorageException.Reason.NOT_FOUND, exception);
            }
            throw unavailable("headObject", objectKey, exception);
        } catch (SdkException exception) {
            throw unavailable("headObject", objectKey, exception);
        }
    }

    @Override
    public void delete(String objectKey) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .build();
        try {
            s3Client.deleteObject(request);
        } catch (S3Exception exception) {
            if (exception.statusCode() != 404) {
                throw unavailable("deleteObject", objectKey, exception);
            }
        } catch (SdkException exception) {
            throw unavailable("deleteObject", objectKey, exception);
        }
    }

    private ObjectStorageException unavailable(String operation, String objectKey, SdkException cause) {
        log.warn("object_storage_failed operation={} objectKey={} errorType={}",
                operation, objectKey, cause.getClass().getSimpleName());
        return new ObjectStorageException(ObjectStorageException.Reason.UNAVAILABLE, cause);
    }
}
