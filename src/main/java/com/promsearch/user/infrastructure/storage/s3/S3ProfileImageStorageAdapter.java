package com.promsearch.user.infrastructure.storage.s3;

import com.promsearch.global.infrastructure.storage.s3.S3StorageProperties;
import com.promsearch.user.application.port.out.profileimage.DeleteProfileImageObjectPort;
import com.promsearch.user.application.port.out.profileimage.LoadProfileImageObjectMetadataPort;
import com.promsearch.user.application.port.out.profileimage.PresignProfileImageUploadPort;
import com.promsearch.user.application.port.out.profileimage.ResolveProfileImageUrlPort;
import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import java.net.URI;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * AWS S3를 이용해 프로필 이미지 Presigned PUT 발급, 메타데이터 조회, URL 변환 및 삭제를 수행합니다.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class S3ProfileImageStorageAdapter implements
        PresignProfileImageUploadPort,
        LoadProfileImageObjectMetadataPort,
        ResolveProfileImageUrlPort,
        DeleteProfileImageObjectPort {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3StorageProperties properties;

    @Override
    public PresignedUpload presignPut(String objectKey, String contentType, long contentLength) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .contentType(contentType)
                .contentLength(contentLength)
                .ifNoneMatch("*")
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(properties.uploadUrlExpiration())
                .putObjectRequest(putRequest)
                .build();

        try {
            PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
            return new PresignedUpload(
                    URI.create(presigned.url().toString()),
                    Instant.now().plus(properties.uploadUrlExpiration())
            );
        } catch (SdkException e) {
            throw storageUnavailable("presignPut", objectKey, e);
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
            return new StoredObjectMetadata(response.contentLength(), response.contentType());
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new UserDomainException(UserErrorCode.PROFILE_IMAGE_UPLOAD_NOT_FOUND);
            }
            throw storageUnavailable("headObject", objectKey, e);
        } catch (SdkException e) {
            throw storageUnavailable("headObject", objectKey, e);
        }
    }

    @Override
    public String resolve(String objectKey) {
        return properties.resolvedProfilePublicBaseUrl() + "/" + objectKey;
    }

    @Override
    public void delete(String objectKey) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .build();
        try {
            s3Client.deleteObject(request);
        } catch (S3Exception e) {
            if (e.statusCode() != 404) {
                throw storageUnavailable("deleteObject", objectKey, e);
            }
        } catch (SdkException e) {
            throw storageUnavailable("deleteObject", objectKey, e);
        }
    }

    private UserDomainException storageUnavailable(String operation, String objectKey, SdkException cause) {
        log.warn("profile_image_storage_failed operation={} objectKey={} errorType={}",
                operation, objectKey, cause.getClass().getSimpleName());
        return new UserDomainException(UserErrorCode.PROFILE_IMAGE_STORAGE_UNAVAILABLE);
    }
}
