package com.promsearch.user.application.service.command;

import com.promsearch.user.application.port.out.profileimage.GenerateProfileImageObjectKeyPort;
import com.promsearch.user.application.port.out.profileimage.ProfileImageDeliveryPort;
import com.promsearch.user.application.port.out.profileimage.ProfileImageStoragePort;
import com.promsearch.user.application.port.out.profileimage.ProfileImageStoragePort.PresignedUpload;
import com.promsearch.user.application.port.out.profileimage.ProfileImageStoragePort.StoredObjectMetadata;
import com.promsearch.user.application.port.out.profileimage.ScheduleProfileImageDeletionPort;
import com.promsearch.user.application.port.out.user.LoadUserPort;
import com.promsearch.user.application.port.out.user.SaveUserPort;
import com.promsearch.user.application.usecase.CompleteProfileImageUploadUseCase;
import com.promsearch.user.application.usecase.IssueProfileImageUploadUrlUseCase;
import com.promsearch.user.application.usecase.RemoveProfileImageUseCase;
import com.promsearch.user.application.usecase.dto.CompleteProfileImageUploadCommand;
import com.promsearch.user.application.usecase.dto.IssueProfileImageUploadUrlCommand;
import com.promsearch.user.application.usecase.dto.ProfileImageUploadUrlInfo;
import com.promsearch.user.application.usecase.dto.UserInfo;
import com.promsearch.user.domain.User;
import com.promsearch.user.domain.enums.ProfileImageContentType;
import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import java.util.Arrays;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 프로필 이미지 업로드 수명주기를 조정하는 애플리케이션 서비스.
 *
 * <p>클라이언트 직접 업로드를 위해 URL을 발급하고, 업로드 완료 시 객체 소유권과
 * 메타데이터를 검증한 뒤 사용자에게 연결한다. 이미지 교체와 제거 과정에서는 DB 변경이
 * 커밋된 후 기존 저장 객체가 삭제되도록 예약한다.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ProfileImageCommandService implements
        IssueProfileImageUploadUrlUseCase,
        CompleteProfileImageUploadUseCase,
        RemoveProfileImageUseCase {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final GenerateProfileImageObjectKeyPort objectKeyGenerator;
    private final ProfileImageStoragePort imageStorage;
    private final ProfileImageDeliveryPort imageDelivery;
    private final ScheduleProfileImageDeletionPort imageDeletionScheduler;

    /**
     * 파일 정책을 먼저 검증하고 사용자 전용 S3 객체 키와 Presigned PUT URL을 발급한다.
     *
     * <p>이 단계에서는 DB에 이미지 키를 저장하지 않는다. 실제 업로드 성공 여부는
     * {@link #complete(CompleteProfileImageUploadCommand)}에서 확인한다.</p>
     */
    @Override
    public ProfileImageUploadUrlInfo issue(IssueProfileImageUploadUrlCommand command) {
        validateIssueCommand(command);
        loadUserPort.getById(command.userId());

        ProfileImageContentType contentType =
                ProfileImageContentType.fromMimeType(command.contentType());
        String objectKey = objectKeyGenerator.generate(command.userId(), contentType);
        PresignedUpload upload = imageStorage.presignPut(
                objectKey,
                contentType.getMimeType(),
                command.fileSize()
        );
        return new ProfileImageUploadUrlInfo(
                objectKey,
                upload.uploadUrl(),
                contentType.getMimeType(),
                command.fileSize(),
                upload.expiresAt()
        );
    }

    /**
     * 직접 업로드된 객체를 검증하고 사용자의 현재 프로필 이미지로 확정한다.
     *
     * <p>발급된 키의 소유권, 확장자, 크기, MIME 타입을 순서대로 검증한다. 검증을 통과하면
     * 소셜 제공자 URL을 제거하고 S3 객체 키를 저장하며, 이전 S3 이미지는 커밋 후 정리한다.</p>
     */
    @Override
    public UserInfo complete(CompleteProfileImageUploadCommand command) {
        if (!objectKeyGenerator.isOwnedBy(command.objectKey(), command.userId())) {
            throw new UserDomainException(UserErrorCode.PROFILE_IMAGE_NOT_OWNED);
        }

        StoredObjectMetadata metadata = imageStorage.getMetadata(command.objectKey());
        if (!matchesExpectedMetadata(command.objectKey(), metadata)) {
            deleteInvalidUpload(command.objectKey());
            throw new UserDomainException(UserErrorCode.PROFILE_IMAGE_UPLOAD_METADATA_MISMATCH);
        }

        User currentUser = loadUserPort.getById(command.userId());
        String previousObjectKey = currentUser.getProfileImageObjectKey();
        User savedUser = saveUserPort.update(currentUser.changeProfileImage(command.objectKey()));
        if (previousObjectKey != null && !previousObjectKey.equals(command.objectKey())) {
            imageDeletionScheduler.afterCommit(previousObjectKey);
        }
        return UserInfo.from(savedUser, resolveImageUrl(savedUser));
    }

    /**
     * 외부 URL과 S3 객체 키를 모두 해제하고, 기존 S3 객체는 DB 커밋 후 삭제한다.
     */
    @Override
    public void remove(Long userId) {
        User currentUser = loadUserPort.getById(userId);
        String previousObjectKey = currentUser.getProfileImageObjectKey();
        saveUserPort.update(currentUser.removeProfileImage());
        imageDeletionScheduler.afterCommit(previousObjectKey);
    }

    /**
     * 프로필 이미지로 허용된 MIME 타입인지와 5MB 이하의 유효한 크기인지 검사한다.
     */
    private void validateFile(String contentType, long fileSize) {
        ProfileImageContentType.fromMimeType(contentType);
        if (fileSize <= 0 || fileSize > MAX_FILE_SIZE) {
            throw new UserDomainException(UserErrorCode.INVALID_PROFILE_IMAGE_FILE_SIZE);
        }
    }

    private void validateIssueCommand(IssueProfileImageUploadUrlCommand command) {
        if (command == null || command.userId() == null || command.userId() <= 0) {
            throw new UserDomainException(UserErrorCode.INVALID_ID);
        }
        validateFile(command.contentType(), command.fileSize());
    }

    /**
     * Object Key 확장자와 실제 S3 메타데이터가 허용된 형식·최대 크기 정책을 만족하는지 확인한다.
     */
    private boolean matchesExpectedMetadata(String objectKey, StoredObjectMetadata metadata) {
        if (metadata == null
                || metadata.contentLength() <= 0
                || metadata.contentLength() > MAX_FILE_SIZE
                || metadata.contentType() == null) {
            return false;
        }
        String normalizedContentType = metadata.contentType().trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(ProfileImageContentType.values())
                .anyMatch(type -> objectKey.endsWith("." + type.getExtension())
                        && normalizedContentType.equals(type.getMimeType()));
    }

    private void deleteInvalidUpload(String objectKey) {
        try {
            imageStorage.delete(objectKey);
        } catch (RuntimeException exception) {
            log.warn("invalid_profile_image_cleanup_failed objectKey={} errorType={}",
                    objectKey, exception.getClass().getSimpleName());
        }
    }

    /**
     * 변경 결과에 즉시 사용할 수 있도록 현재 저장 방식에 맞는 전달 URL을 만든다.
     */
    private String resolveImageUrl(User user) {
        return imageDelivery.resolve(user.getProfileImageUrl(), user.getProfileImageObjectKey());
    }
}
