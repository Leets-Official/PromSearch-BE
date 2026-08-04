package com.promsearch.user.application.service.command;

import com.promsearch.user.application.port.out.profileimage.DeleteProfileImageObjectPort;
import com.promsearch.user.application.port.out.profileimage.GenerateProfileImageObjectKeyPort;
import com.promsearch.user.application.port.out.profileimage.LoadProfileImageObjectMetadataPort;
import com.promsearch.user.application.port.out.profileimage.LoadProfileImageObjectMetadataPort.StoredObjectMetadata;
import com.promsearch.user.application.port.out.profileimage.PresignProfileImageUploadPort;
import com.promsearch.user.application.port.out.profileimage.PresignProfileImageUploadPort.PresignedUpload;
import com.promsearch.user.application.port.out.profileimage.ResolveProfileImageUrlPort;
import com.promsearch.user.application.port.out.user.LoadUserPort;
import com.promsearch.user.application.port.out.user.SaveUserPort;
import com.promsearch.user.application.usecase.CompleteProfileImageUploadUseCase;
import com.promsearch.user.application.usecase.DeleteProfileImageUseCase;
import com.promsearch.user.application.usecase.IssueProfileImageUploadUrlUseCase;
import com.promsearch.user.application.usecase.dto.CompleteProfileImageUploadCommand;
import com.promsearch.user.application.usecase.dto.IssueProfileImageUploadUrlCommand;
import com.promsearch.user.application.usecase.dto.ProfileImageCleanupEvent;
import com.promsearch.user.application.usecase.dto.ProfileImageInfo;
import com.promsearch.user.application.usecase.dto.ProfileImageUploadUrlInfo;
import com.promsearch.user.domain.User;
import com.promsearch.user.domain.enums.ProfileImageContentType;
import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 프로필 이미지 업로드 URL 발급, 적용·교체, 삭제 흐름을 처리하는 명령 서비스입니다.
 * 저장소 객체 삭제는 DB 상태 변경이 커밋된 뒤 정리 이벤트를 통해 수행합니다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ProfileImageCommandService implements
        IssueProfileImageUploadUrlUseCase,
        CompleteProfileImageUploadUseCase,
        DeleteProfileImageUseCase {

    static final long MAX_PROFILE_IMAGE_FILE_SIZE = 5L * 1024 * 1024;

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final GenerateProfileImageObjectKeyPort generateObjectKeyPort;
    private final PresignProfileImageUploadPort presignUploadPort;
    private final LoadProfileImageObjectMetadataPort loadObjectMetadataPort;
    private final ResolveProfileImageUrlPort resolveProfileImageUrlPort;
    private final DeleteProfileImageObjectPort deleteProfileImageObjectPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public ProfileImageUploadUrlInfo issue(IssueProfileImageUploadUrlCommand command) {
        validateIssueCommand(command);
        loadUserPort.getById(command.userId());

        ProfileImageContentType contentType = ProfileImageContentType.fromMimeType(command.contentType());
        String objectKey = generateObjectKeyPort.generate(
                command.userId(),
                UUID.randomUUID(),
                contentType
        );
        PresignedUpload presigned = presignUploadPort.presignPut(
                objectKey,
                contentType.getMimeType(),
                command.fileSize()
        );

        log.info("profile_image_upload_url_issued userId={} objectKey={}", command.userId(), objectKey);
        return new ProfileImageUploadUrlInfo(
                objectKey,
                presigned.uploadUrl(),
                contentType.getMimeType(),
                command.fileSize(),
                presigned.expiresAt()
        );
    }

    @Override
    public ProfileImageInfo complete(CompleteProfileImageUploadCommand command) {
        if (!generateObjectKeyPort.isOwnedBy(command.userId(), command.objectKey())) {
            throw new UserDomainException(UserErrorCode.PROFILE_IMAGE_NOT_OWNED);
        }

        User user = loadUserPort.getById(command.userId());
        StoredObjectMetadata metadata = loadObjectMetadataPort.getMetadata(command.objectKey());
        if (!isValidMetadata(metadata)) {
            deleteInvalidUpload(command.objectKey());
            throw new UserDomainException(UserErrorCode.PROFILE_IMAGE_UPLOAD_METADATA_MISMATCH);
        }

        String profileImageUrl = resolveProfileImageUrlPort.resolve(command.objectKey());
        String previousObjectKey = user.getProfileImageObjectKey();
        User updated = saveUserPort.update(user.replaceProfileImage(profileImageUrl, command.objectKey()));

        if (previousObjectKey != null && !previousObjectKey.equals(command.objectKey())) {
            eventPublisher.publishEvent(new ProfileImageCleanupEvent(previousObjectKey));
        }

        log.info("profile_image_upload_completed userId={} objectKey={}", command.userId(), command.objectKey());
        return new ProfileImageInfo(updated.getProfileImageUrl());
    }

    @Override
    public void delete(Long userId) {
        User user = loadUserPort.getById(userId);
        if (user.getProfileImageUrl() == null) {
            return;
        }

        String previousObjectKey = user.getProfileImageObjectKey();
        saveUserPort.update(user.removeProfileImage());
        if (previousObjectKey != null) {
            eventPublisher.publishEvent(new ProfileImageCleanupEvent(previousObjectKey));
        }
        log.info("profile_image_deleted userId={}", userId);
    }

    private void validateIssueCommand(IssueProfileImageUploadUrlCommand command) {
        if (command == null || command.userId() == null || command.userId() <= 0) {
            throw new UserDomainException(UserErrorCode.INVALID_ID);
        }
        if (command.fileSize() <= 0 || command.fileSize() > MAX_PROFILE_IMAGE_FILE_SIZE) {
            throw new UserDomainException(UserErrorCode.INVALID_PROFILE_IMAGE_FILE_SIZE);
        }
    }

    private boolean isValidMetadata(StoredObjectMetadata metadata) {
        if (metadata == null
                || metadata.contentLength() <= 0
                || metadata.contentLength() > MAX_PROFILE_IMAGE_FILE_SIZE
                || metadata.contentType() == null) {
            return false;
        }

        String normalized = metadata.contentType().trim().toLowerCase(Locale.ROOT);
        return normalized.equals(ProfileImageContentType.JPEG.getMimeType())
                || normalized.equals(ProfileImageContentType.PNG.getMimeType());
    }

    private void deleteInvalidUpload(String objectKey) {
        try {
            deleteProfileImageObjectPort.delete(objectKey);
        } catch (RuntimeException e) {
            log.warn("invalid_profile_image_cleanup_failed objectKey={} errorType={}",
                    objectKey, e.getClass().getSimpleName());
        }
    }
}
