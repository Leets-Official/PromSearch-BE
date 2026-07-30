package com.promsearch.user.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.promsearch.user.application.port.out.profileimage.GenerateProfileImageObjectKeyPort;
import com.promsearch.user.application.port.out.profileimage.ProfileImageDeliveryPort;
import com.promsearch.user.application.port.out.profileimage.ProfileImageStoragePort;
import com.promsearch.user.application.port.out.profileimage.ProfileImageStoragePort.PresignedUpload;
import com.promsearch.user.application.port.out.profileimage.ProfileImageStoragePort.StoredObjectMetadata;
import com.promsearch.user.application.port.out.profileimage.ScheduleProfileImageDeletionPort;
import com.promsearch.user.application.port.out.user.LoadUserPort;
import com.promsearch.user.application.port.out.user.SaveUserPort;
import com.promsearch.user.application.usecase.dto.CompleteProfileImageUploadCommand;
import com.promsearch.user.application.usecase.dto.IssueProfileImageUploadUrlCommand;
import com.promsearch.user.application.usecase.dto.ProfileImageUploadUrlInfo;
import com.promsearch.user.application.usecase.dto.UserInfo;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProfileImageCommandServiceTest {

    private LoadUserPort loadUserPort;
    private SaveUserPort saveUserPort;
    private GenerateProfileImageObjectKeyPort objectKeyGenerator;
    private ProfileImageStoragePort imageStorage;
    private ProfileImageDeliveryPort imageDelivery;
    private ScheduleProfileImageDeletionPort deletionScheduler;
    private ProfileImageCommandService service;

    @BeforeEach
    void setUp() {
        loadUserPort = mock(LoadUserPort.class);
        saveUserPort = mock(SaveUserPort.class);
        objectKeyGenerator = mock(GenerateProfileImageObjectKeyPort.class);
        imageStorage = mock(ProfileImageStoragePort.class);
        imageDelivery = mock(ProfileImageDeliveryPort.class);
        deletionScheduler = mock(ScheduleProfileImageDeletionPort.class);
        service = new ProfileImageCommandService(
                loadUserPort,
                saveUserPort,
                objectKeyGenerator,
                imageStorage,
                imageDelivery,
                deletionScheduler
        );
    }

    @Test
    void issueUsesProfilePathStrategyAndCommonStoragePort() {
        User user = socialUser(null);
        String objectKey = "profile-images/1/123e4567-e89b-12d3-a456-426614174000.jpg";
        Instant expiresAt = Instant.parse("2026-07-30T12:10:00Z");
        when(loadUserPort.getById(1L)).thenReturn(user);
        when(objectKeyGenerator.generate(1L, ProfileImageContentType.JPEG)).thenReturn(objectKey);
        when(imageStorage.presignPut(objectKey, "image/jpeg"))
                .thenReturn(new PresignedUpload(URI.create("https://upload.test"), expiresAt));

        ProfileImageUploadUrlInfo result = service.issue(
                IssueProfileImageUploadUrlCommand.of(1L, "image/jpeg", 1_024)
        );

        assertThat(result.objectKey()).isEqualTo(objectKey);
        assertThat(result.uploadUrl()).isEqualTo(URI.create("https://upload.test"));
        assertThat(result.expiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void completeReplacesSocialUrlWithManagedObjectKey() {
        String objectKey = "profile-images/1/123e4567-e89b-12d3-a456-426614174000.jpg";
        User user = socialUser(null);
        when(objectKeyGenerator.isOwnedBy(objectKey, 1L)).thenReturn(true);
        when(imageStorage.getMetadata(objectKey))
                .thenReturn(new StoredObjectMetadata(1_024, "image/jpeg"));
        when(loadUserPort.getById(1L)).thenReturn(user);
        when(saveUserPort.update(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(imageDelivery.resolve(null, objectKey)).thenReturn("https://signed.test/profile");

        UserInfo result = service.complete(
                CompleteProfileImageUploadCommand.of(1L, objectKey, "image/jpeg", 1_024)
        );

        assertThat(result.profileImageUrl()).isEqualTo("https://signed.test/profile");
        verify(saveUserPort).update(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.getProfileImageUrl() == null
                        && objectKey.equals(saved.getProfileImageObjectKey())
        ));
    }

    @Test
    void completeRejectsAnotherUsersObjectKeyBeforeStorageLookup() {
        String objectKey = "profile-images/2/123e4567-e89b-12d3-a456-426614174000.jpg";
        when(objectKeyGenerator.isOwnedBy(objectKey, 1L)).thenReturn(false);

        assertThatThrownBy(() -> service.complete(
                CompleteProfileImageUploadCommand.of(1L, objectKey, "image/jpeg", 1_024)
        ))
                .isInstanceOf(UserDomainException.class)
                .extracting("baseCode")
                .isEqualTo(UserErrorCode.PROFILE_IMAGE_NOT_OWNED);
        verify(imageStorage, never()).getMetadata(any());
    }

    @Test
    void removeSchedulesManagedObjectDeletionAfterUserUpdate() {
        String objectKey = "profile-images/1/123e4567-e89b-12d3-a456-426614174000.jpg";
        User user = socialUser(objectKey);
        when(loadUserPort.getById(1L)).thenReturn(user);
        when(saveUserPort.update(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.remove(1L);

        verify(saveUserPort).update(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.getProfileImageUrl() == null && saved.getProfileImageObjectKey() == null
        ));
        verify(deletionScheduler).afterCommit(objectKey);
    }

    private User socialUser(String objectKey) {
        Instant now = Instant.parse("2026-07-30T12:00:00Z");
        return User.reconstruct(
                new UserId(1L),
                "user@test.com",
                "encoded",
                "nickname",
                "name",
                "https://social.test/profile.png",
                objectKey,
                100L,
                UserRole.USER,
                UserGrade.NORMAL,
                UserStatus.ACTIVE,
                now,
                now
        );
    }
}
