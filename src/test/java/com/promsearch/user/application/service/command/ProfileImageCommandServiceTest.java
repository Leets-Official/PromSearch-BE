package com.promsearch.user.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.promsearch.user.application.port.out.profileimage.DeleteProfileImageObjectPort;
import com.promsearch.user.application.port.out.profileimage.GenerateProfileImageObjectKeyPort;
import com.promsearch.user.application.port.out.profileimage.LoadProfileImageObjectMetadataPort;
import com.promsearch.user.application.port.out.profileimage.LoadProfileImageObjectMetadataPort.StoredObjectMetadata;
import com.promsearch.user.application.port.out.profileimage.PresignProfileImageUploadPort;
import com.promsearch.user.application.port.out.profileimage.PresignProfileImageUploadPort.PresignedUpload;
import com.promsearch.user.application.port.out.profileimage.ResolveProfileImageUrlPort;
import com.promsearch.user.application.port.out.user.LoadUserPort;
import com.promsearch.user.application.port.out.user.SaveUserPort;
import com.promsearch.user.application.usecase.dto.CompleteProfileImageUploadCommand;
import com.promsearch.user.application.usecase.dto.IssueProfileImageUploadUrlCommand;
import com.promsearch.user.application.usecase.dto.ProfileImageCleanupEvent;
import com.promsearch.user.application.usecase.dto.ProfileImageInfo;
import com.promsearch.user.application.usecase.dto.ProfileImageUploadUrlInfo;
import com.promsearch.user.domain.User;
import com.promsearch.user.domain.User.UserId;
import com.promsearch.user.domain.enums.ProfileImageContentType;
import com.promsearch.user.domain.enums.UserGrade;
import com.promsearch.user.domain.enums.UserRole;
import com.promsearch.user.domain.enums.UserStatus;
import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class ProfileImageCommandServiceTest {

    private LoadUserPort loadUserPort;
    private SaveUserPort saveUserPort;
    private GenerateProfileImageObjectKeyPort generateObjectKeyPort;
    private PresignProfileImageUploadPort presignUploadPort;
    private LoadProfileImageObjectMetadataPort loadObjectMetadataPort;
    private ResolveProfileImageUrlPort resolveProfileImageUrlPort;
    private DeleteProfileImageObjectPort deleteProfileImageObjectPort;
    private ApplicationEventPublisher eventPublisher;
    private ProfileImageCommandService service;

    @BeforeEach
    void setUp() {
        loadUserPort = mock(LoadUserPort.class);
        saveUserPort = mock(SaveUserPort.class);
        generateObjectKeyPort = mock(GenerateProfileImageObjectKeyPort.class);
        presignUploadPort = mock(PresignProfileImageUploadPort.class);
        loadObjectMetadataPort = mock(LoadProfileImageObjectMetadataPort.class);
        resolveProfileImageUrlPort = mock(ResolveProfileImageUrlPort.class);
        deleteProfileImageObjectPort = mock(DeleteProfileImageObjectPort.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new ProfileImageCommandService(
                loadUserPort,
                saveUserPort,
                generateObjectKeyPort,
                presignUploadPort,
                loadObjectMetadataPort,
                resolveProfileImageUrlPort,
                deleteProfileImageObjectPort,
                eventPublisher
        );
    }

    @DisplayName("사용자 전용 Object Key와 Presigned PUT URL을 발급한다")
    @Test
    void issueUploadUrl() {
        String objectKey = "profiles/12/123e4567-e89b-12d3-a456-426614174000.jpg";
        Instant expiresAt = Instant.parse("2026-08-04T12:10:00Z");
        when(loadUserPort.getById(12L)).thenReturn(user(null, null));
        when(generateObjectKeyPort.generate(eq(12L), any(UUID.class), eq(ProfileImageContentType.JPEG)))
                .thenReturn(objectKey);
        when(presignUploadPort.presignPut(objectKey, "image/jpeg", 1_024L))
                .thenReturn(new PresignedUpload(URI.create("https://s3.example.com/upload"), expiresAt));

        ProfileImageUploadUrlInfo info = service.issue(
                new IssueProfileImageUploadUrlCommand(12L, "image/jpeg", 1_024L)
        );

        assertThat(info.objectKey()).isEqualTo(objectKey);
        assertThat(info.uploadUrl()).hasToString("https://s3.example.com/upload");
        assertThat(info.contentType()).isEqualTo("image/jpeg");
        assertThat(info.contentLength()).isEqualTo(1_024L);
        assertThat(info.expiresAt()).isEqualTo(expiresAt);
    }

    @DisplayName("업로드를 검증해 프로필을 교체하고 기존 자사 이미지를 커밋 후 정리 대상으로 발행한다")
    @Test
    void completeReplacesProfileImage() {
        String oldKey = "profiles/12/00000000-0000-0000-0000-000000000001.jpg";
        String newKey = "profiles/12/00000000-0000-0000-0000-000000000002.png";
        when(generateObjectKeyPort.isOwnedBy(12L, newKey)).thenReturn(true);
        when(loadUserPort.getById(12L)).thenReturn(user("https://cdn.test/old.jpg", oldKey));
        when(loadObjectMetadataPort.getMetadata(newKey))
                .thenReturn(new StoredObjectMetadata(2_048L, "image/png"));
        when(resolveProfileImageUrlPort.resolve(newKey)).thenReturn("https://cdn.test/" + newKey);
        when(saveUserPort.update(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProfileImageInfo info = service.complete(new CompleteProfileImageUploadCommand(12L, newKey));

        assertThat(info.profileImageUrl()).isEqualTo("https://cdn.test/" + newKey);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isEqualTo(new ProfileImageCleanupEvent(oldKey));
    }

    @DisplayName("다른 사용자의 Object Key는 S3 조회 전에 거부한다")
    @Test
    void completeRejectsUnownedObjectKey() {
        String objectKey = "profiles/99/00000000-0000-0000-0000-000000000002.png";
        when(generateObjectKeyPort.isOwnedBy(12L, objectKey)).thenReturn(false);

        assertThatThrownBy(() -> service.complete(new CompleteProfileImageUploadCommand(12L, objectKey)))
                .isInstanceOf(UserDomainException.class)
                .extracting("baseCode")
                .isEqualTo(UserErrorCode.PROFILE_IMAGE_NOT_OWNED);

        verify(loadObjectMetadataPort, never()).getMetadata(any());
    }

    @DisplayName("허용 크기를 초과한 업로드 객체는 삭제하고 적용하지 않는다")
    @Test
    void completeDeletesInvalidUpload() {
        String objectKey = "profiles/12/00000000-0000-0000-0000-000000000002.png";
        when(generateObjectKeyPort.isOwnedBy(12L, objectKey)).thenReturn(true);
        when(loadUserPort.getById(12L)).thenReturn(user(null, null));
        when(loadObjectMetadataPort.getMetadata(objectKey))
                .thenReturn(new StoredObjectMetadata(5L * 1024 * 1024 + 1, "image/png"));

        assertThatThrownBy(() -> service.complete(new CompleteProfileImageUploadCommand(12L, objectKey)))
                .isInstanceOf(UserDomainException.class)
                .extracting("baseCode")
                .isEqualTo(UserErrorCode.PROFILE_IMAGE_UPLOAD_METADATA_MISMATCH);

        verify(deleteProfileImageObjectPort).delete(objectKey);
        verify(saveUserPort, never()).update(any());
    }

    @DisplayName("외부 OAuth 프로필 이미지는 DB 연결만 제거하고 S3 삭제 이벤트를 발행하지 않는다")
    @Test
    void deleteExternalProfileImage() {
        when(loadUserPort.getById(12L)).thenReturn(user("https://oauth.example.com/profile.jpg", null));
        when(saveUserPort.update(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.delete(12L);

        verify(saveUserPort).update(any(User.class));
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    private User user(String profileImageUrl, String profileImageObjectKey) {
        Instant now = Instant.parse("2026-08-04T12:00:00Z");
        return User.reconstruct(
                new UserId(12L),
                "user@example.com",
                "encoded-password",
                "nickname",
                "name",
                profileImageUrl,
                profileImageObjectKey,
                0L,
                UserRole.USER,
                UserGrade.NORMAL,
                UserStatus.ACTIVE,
                now,
                now
        );
    }
}
