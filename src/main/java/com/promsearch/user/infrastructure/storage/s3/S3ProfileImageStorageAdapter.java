package com.promsearch.user.infrastructure.storage.s3;

import com.promsearch.common.infrastructure.storage.ObjectStorageException;
import com.promsearch.common.infrastructure.storage.ObjectStorageOperations;
import com.promsearch.common.infrastructure.storage.ObjectStorageOperations.PresignedPutOptions;
import com.promsearch.user.application.port.out.profileimage.ProfileImageDeliveryPort;
import com.promsearch.user.application.port.out.profileimage.ProfileImageStoragePort;
import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 사용자 프로필 이미지 포트를 공통 Object Storage 동작에 연결하는 Adapter.
 */
@Component
@RequiredArgsConstructor
public class S3ProfileImageStorageAdapter implements ProfileImageStoragePort, ProfileImageDeliveryPort {

    private final ObjectStorageOperations objectStorage;

    /**
     * 공통 저장소의 업로드 결과를 프로필 이미지 포트의 결과 타입으로 변환한다.
     */
    @Override
    public PresignedUpload presignPut(String objectKey, String contentType, long contentLength) {
        try {
            ObjectStorageOperations.PresignedUpload upload =
                    objectStorage.presignPut(
                            objectKey,
                            PresignedPutOptions.immutable(contentType, contentLength)
                    );
            return new PresignedUpload(upload.uploadUrl(), upload.expiresAt());
        } catch (ObjectStorageException exception) {
            throw unavailable();
        }
    }

    /**
     * S3 관리 프로필 이미지를 클라이언트에 전달할 서명 URL로 변환한다.
     */
    @Override
    public String presignGet(String objectKey) {
        try {
            return objectStorage.presignGet(objectKey);
        } catch (ObjectStorageException exception) {
            throw unavailable();
        }
    }

    /**
     * 업로드 완료 검증에 필요한 크기와 MIME 타입만 프로필 도메인에 전달한다.
     */
    @Override
    public StoredObjectMetadata getMetadata(String objectKey) {
        try {
            ObjectStorageOperations.StoredObjectMetadata metadata =
                    objectStorage.getMetadata(objectKey);
            return new StoredObjectMetadata(metadata.contentLength(), metadata.contentType());
        } catch (ObjectStorageException exception) {
            if (exception.getReason() == ObjectStorageException.Reason.NOT_FOUND) {
                throw new UserDomainException(UserErrorCode.PROFILE_IMAGE_UPLOAD_NOT_FOUND);
            }
            throw unavailable();
        }
    }

    /**
     * 공통 저장소 삭제 실패를 사용자 도메인의 저장소 오류로 변환한다.
     */
    @Override
    public void delete(String objectKey) {
        try {
            objectStorage.delete(objectKey);
        } catch (ObjectStorageException exception) {
            throw unavailable();
        }
    }

    /**
     * 사용자가 직접 올린 객체 키를 우선하고, 없을 때만 소셜 제공자 URL을 반환한다.
     */
    @Override
    public String resolve(String externalUrl, String objectKey) {
        return objectKey == null || objectKey.isBlank() ? externalUrl : presignGet(objectKey);
    }

    /**
     * 인프라 오류가 API 경계 밖으로 노출되지 않도록 사용자 도메인 예외로 치환한다.
     */
    private UserDomainException unavailable() {
        return new UserDomainException(UserErrorCode.PROFILE_IMAGE_STORAGE_UNAVAILABLE);
    }
}
