package com.promsearch.prompt.infrastructure.storage.s3;

import com.promsearch.prompt.application.port.out.storage.DeletePromptImageObjectPort;
import com.promsearch.prompt.application.port.out.storage.LoadPromptImageObjectMetadataPort;
import com.promsearch.prompt.application.port.out.storage.PresignPromptImageDownloadPort;
import com.promsearch.prompt.application.port.out.storage.PresignPromptImageUploadPort;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
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
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

/** Presign·HeadObject·DeleteObject S3 어댑터 */
@Component
@Slf4j
@RequiredArgsConstructor
public class S3PromptImageStorageAdapter implements
        PresignPromptImageUploadPort,
        PresignPromptImageDownloadPort,
        LoadPromptImageObjectMetadataPort,
        DeletePromptImageObjectPort {

    // TODO: READY 이미지 조회에는 별도 배포 URL 포트를 두고 CloudFront OAC로 S3를 비공개 유지
    // 비공개 이미지 정책이 필요하면 CloudFront Signed URL 또는 Signed Cookie 적용

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3StorageProperties properties;

    /** 버킷·Object Key·Content-Type·만료 시간 기반 Presigned PUT URL 반환 */
    @Override
    public PresignedUpload presignPut(String objectKey, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .contentType(contentType)
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(properties.uploadUrlExpiration())
                .putObjectRequest(putObjectRequest)
                .build();

        try {
            Instant expiresAt = Instant.now().plus(properties.uploadUrlExpiration());
            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
            return new PresignedUpload(
                    URI.create(presignedRequest.url().toString()),
                    expiresAt
            );
        } catch (SdkException e) {
            throw storageUnavailable("presignPut", objectKey, e);
        }
    }

    @Override
    public String presignGet(String objectKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(properties.uploadUrlExpiration())
                .getObjectRequest(getObjectRequest)
                .build();

        try {
            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (SdkException e) {
            throw storageUnavailable("presignGet", objectKey, e);
        }
    }

    /** HeadObject 응답을 객체 메타데이터로 변환 */
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
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new PromptDomainException(PromptErrorCode.IMAGE_UPLOAD_NOT_FOUND);
            }
            throw storageUnavailable("headObject", objectKey, e);
        } catch (SdkException e) {
            throw storageUnavailable("headObject", objectKey, e);
        }
    }

    /** 검증 실패 객체 삭제 및 404 멱등 처리 */
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

    /** AWS SDK 세부 오류는 로그로 남기고 상위 계층에는 공통 저장소 오류만 노출 */
    private PromptDomainException storageUnavailable(
            String operation,
            String objectKey,
            SdkException cause
    ) {
        log.warn("prompt_image_storage_failed operation={} objectKey={} errorType={}",
                operation, objectKey, cause.getClass().getSimpleName());
        return new PromptDomainException(PromptErrorCode.IMAGE_STORAGE_UNAVAILABLE);
    }
}
