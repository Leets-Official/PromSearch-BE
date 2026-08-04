package com.promsearch.prompt.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.promsearch.prompt.application.port.out.promptimage.LoadPromptImagePort;
import com.promsearch.prompt.application.port.out.promptimage.SavePromptImagePort;
import com.promsearch.prompt.application.port.out.promptimage.SavePromptImageWatermarkJobPort;
import com.promsearch.prompt.application.port.out.storage.DeletePromptImageObjectPort;
import com.promsearch.prompt.application.port.out.storage.GeneratePromptImageObjectKeyPort;
import com.promsearch.prompt.application.port.out.storage.LoadPromptImageObjectMetadataPort;
import com.promsearch.prompt.application.port.out.storage.LoadPromptImageObjectMetadataPort.StoredObjectMetadata;
import com.promsearch.prompt.application.port.out.storage.PresignPromptImageUploadPort;
import com.promsearch.prompt.application.port.out.storage.PresignPromptImageUploadPort.PresignedUpload;
import com.promsearch.prompt.application.usecase.dto.CompletePromptImageUploadCommand;
import com.promsearch.prompt.application.usecase.dto.IssuePromptImageUploadUrlsCommand;
import com.promsearch.prompt.application.usecase.dto.PromptImageUploadInfo;
import com.promsearch.prompt.application.usecase.dto.PromptImageUploadUrlsInfo;
import com.promsearch.prompt.application.usecase.dto.PromptImageWatermarkJob;
import com.promsearch.prompt.domain.PromptImage;
import com.promsearch.prompt.domain.enums.PromptImageContentType;
import com.promsearch.prompt.domain.enums.PromptImageStatus;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PromptImageUploadCommandServiceTest {

    @Mock
    private LoadPromptImagePort loadPromptImagePort;
    @Mock
    private SavePromptImagePort savePromptImagePort;
    @Mock
    private GeneratePromptImageObjectKeyPort generatePromptImageObjectKeyPort;
    @Mock
    private PresignPromptImageUploadPort presignPromptImageUploadPort;
    @Mock
    private LoadPromptImageObjectMetadataPort loadPromptImageObjectMetadataPort;
    @Mock
    private DeletePromptImageObjectPort deletePromptImageObjectPort;
    @Mock
    private SavePromptImageWatermarkJobPort savePromptImageWatermarkJobPort;

    private PromptImageUploadCommandService service;

    @BeforeEach
    void setUp() {
        service = new PromptImageUploadCommandService(
                loadPromptImagePort,
                savePromptImagePort,
                generatePromptImageObjectKeyPort,
                presignPromptImageUploadPort,
                loadPromptImageObjectMetadataPort,
                deletePromptImageObjectPort,
                savePromptImageWatermarkJobPort
        );
    }

    @DisplayName("요청 순서대로 이미지 자산과 Presigned PUT URL을 생성한다")
    @Test
    @SuppressWarnings("unchecked")
    void issueUploadUrlsCreatesUploadingAssets() {
        Instant expiresAt = Instant.parse("2026-07-26T01:10:00Z");
        when(generatePromptImageObjectKeyPort.generateOriginal(any(), any(), any()))
                .thenAnswer(invocation -> {
                    Long uploaderId = invocation.getArgument(0);
                    UUID imageId = invocation.getArgument(1);
                    PromptImageContentType contentType = invocation.getArgument(2);
                    return "prompt-images/original/%d/%s.%s".formatted(
                            uploaderId,
                            imageId,
                            contentType.getExtension()
                    );
                });
        when(presignPromptImageUploadPort.presignPut(any(), any(), anyLong()))
                .thenReturn(new PresignedUpload(URI.create("https://s3.example.com/upload"), expiresAt));

        PromptImageUploadUrlsInfo info = service.issue(new IssuePromptImageUploadUrlsCommand(
                1L,
                List.of(
                        new IssuePromptImageUploadUrlsCommand.ImageFile(
                                "first.jpg", "image/jpeg", 1_024, 1_920, 1_080
                        ),
                        new IssuePromptImageUploadUrlsCommand.ImageFile(
                                "second.png", "image/png", 2_048, 1_024, 1_024
                        )
                )
        ));

        ArgumentCaptor<List<PromptImage>> imagesCaptor = ArgumentCaptor.forClass(List.class);
        verify(savePromptImagePort).createAll(imagesCaptor.capture());
        assertThat(imagesCaptor.getValue())
                .extracting(PromptImage::getStatus)
                .containsExactly(PromptImageStatus.UPLOADING, PromptImageStatus.UPLOADING);
        assertThat(imagesCaptor.getValue())
                .extracting(PromptImage::getOriginalFileName)
                .containsExactly("first.jpg", "second.png");
        assertThat(info.images()).hasSize(2);
        assertThat(info.images()).extracting(PromptImageUploadUrlsInfo.UploadTarget::expiresAt)
                .containsOnly(expiresAt);
    }

    @DisplayName("S3 객체의 크기와 타입이 일치하면 UPLOADED 상태로 완료한다")
    @Test
    void completeUploadVerifiesS3Metadata() {
        PromptImage image = uploadingImage(1L);
        Instant uploadedAt = Instant.parse("2026-07-26T01:00:00Z");
        when(loadPromptImagePort.getById(image.getPromptImageId().id())).thenReturn(image);
        when(loadPromptImageObjectMetadataPort.getMetadata(image.getOriginalObjectKey()))
                .thenReturn(new StoredObjectMetadata(1_024, "image/jpeg", "\"etag\"", uploadedAt));
        when(savePromptImagePort.update(image)).thenReturn(image);
        String watermarkedObjectKey =
                "prompt-images/watermarked/1/" + image.getPromptImageId().id() + ".jpg";
        when(generatePromptImageObjectKeyPort.generateWatermarked(
                image.getUploaderId(),
                image.getPromptImageId().id(),
                image.getContentType()
        )).thenReturn(watermarkedObjectKey);

        PromptImageUploadInfo info = service.complete(new CompletePromptImageUploadCommand(
                1L,
                image.getPromptImageId().id()
        ));

        assertThat(info.status()).isEqualTo(PromptImageStatus.UPLOADED);
        assertThat(info.uploadedAt()).isEqualTo(uploadedAt);
        assertThat(image.getEtag()).isEqualTo("\"etag\"");
        verify(savePromptImagePort).update(image);
        ArgumentCaptor<PromptImageWatermarkJob> jobCaptor =
                ArgumentCaptor.forClass(PromptImageWatermarkJob.class);
        verify(savePromptImageWatermarkJobPort).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().imageId()).isEqualTo(image.getPromptImageId().id());
        assertThat(jobCaptor.getValue().originalObjectKey()).isEqualTo(image.getOriginalObjectKey());
        assertThat(jobCaptor.getValue().watermarkedObjectKey()).isEqualTo(watermarkedObjectKey);
        assertThat(jobCaptor.getValue().contentType()).isEqualTo("image/jpeg");
        assertThat(jobCaptor.getValue().processingVersion()).isEqualTo(1);
    }

    @DisplayName("완료 요청 재시도는 S3를 다시 조회하지 않고 기존 결과를 반환한다")
    @Test
    void completeUploadIsIdempotent() {
        PromptImage image = uploadingImage(1L);
        image.completeUpload("\"etag\"", Instant.parse("2026-07-26T01:00:00Z"));
        when(loadPromptImagePort.getById(image.getPromptImageId().id())).thenReturn(image);

        PromptImageUploadInfo info = service.complete(new CompletePromptImageUploadCommand(
                1L,
                image.getPromptImageId().id()
        ));

        assertThat(info.status()).isEqualTo(PromptImageStatus.UPLOADED);
        verify(loadPromptImageObjectMetadataPort, never()).getMetadata(any());
        verify(savePromptImagePort, never()).update(any());
        verify(savePromptImageWatermarkJobPort, never()).save(any());
    }

    @DisplayName("S3 객체 메타데이터가 다르면 객체를 삭제하고 완료를 거절한다")
    @Test
    void completeUploadDeletesMismatchedObject() {
        PromptImage image = uploadingImage(1L);
        when(loadPromptImagePort.getById(image.getPromptImageId().id())).thenReturn(image);
        when(loadPromptImageObjectMetadataPort.getMetadata(image.getOriginalObjectKey()))
                .thenReturn(new StoredObjectMetadata(
                        2_048,
                        "image/png",
                        "\"etag\"",
                        Instant.parse("2026-07-26T01:00:00Z")
                ));

        assertThatThrownBy(() -> service.complete(new CompletePromptImageUploadCommand(
                1L,
                image.getPromptImageId().id()
        )))
                .isInstanceOf(PromptDomainException.class)
                .extracting("baseCode")
                .isEqualTo(PromptErrorCode.IMAGE_UPLOAD_METADATA_MISMATCH);

        verify(deletePromptImageObjectPort).delete(image.getOriginalObjectKey());
        verify(savePromptImagePort, never()).update(any());
        verify(savePromptImageWatermarkJobPort, never()).save(any());
    }

    @DisplayName("다른 사용자는 이미지 업로드를 완료할 수 없다")
    @Test
    void completeUploadChecksOwnershipBeforeS3() {
        PromptImage image = uploadingImage(1L);
        when(loadPromptImagePort.getById(image.getPromptImageId().id())).thenReturn(image);

        assertThatThrownBy(() -> service.complete(new CompletePromptImageUploadCommand(
                2L,
                image.getPromptImageId().id()
        )))
                .isInstanceOf(PromptDomainException.class)
                .extracting("baseCode")
                .isEqualTo(PromptErrorCode.IMAGE_NOT_OWNED);

        verify(loadPromptImageObjectMetadataPort, never()).getMetadata(any());
        verify(savePromptImageWatermarkJobPort, never()).save(any());
    }

    private PromptImage uploadingImage(Long uploaderId) {
        UUID imageId = UUID.randomUUID();
        return PromptImage.prepareUpload(
                imageId,
                uploaderId,
                "prompt-images/original/" + uploaderId + "/" + imageId + ".jpg",
                "result.jpg",
                PromptImageContentType.JPEG,
                1_024,
                1_920,
                1_080
        );
    }
}
