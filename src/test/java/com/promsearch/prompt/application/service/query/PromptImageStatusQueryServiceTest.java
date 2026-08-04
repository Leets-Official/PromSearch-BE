package com.promsearch.prompt.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.promsearch.prompt.application.port.out.promptimage.LoadPromptImagePort;
import com.promsearch.prompt.application.port.out.storage.PresignPromptImageDownloadPort;
import com.promsearch.prompt.application.usecase.dto.GetPromptImageStatusesQuery;
import com.promsearch.prompt.application.usecase.dto.PromptImageStatusesInfo;
import com.promsearch.prompt.domain.PromptImage;
import com.promsearch.prompt.domain.enums.PromptImageContentType;
import com.promsearch.prompt.domain.enums.PromptImageStatus;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PromptImageStatusQueryServiceTest {

    @Mock
    private LoadPromptImagePort loadPromptImagePort;

    @Mock
    private PresignPromptImageDownloadPort presignPromptImageDownloadPort;

    private PromptImageStatusQueryService service;

    @BeforeEach
    void setUp() {
        service = new PromptImageStatusQueryService(loadPromptImagePort, presignPromptImageDownloadPort);
    }

    @DisplayName("이미지 상태를 요청한 imageId 순서대로 반환한다")
    @Test
    void getStatusesReturnsImagesInRequestOrder() {
        PromptImage first = readyImage(1L, "first.jpg");
        PromptImage second = failedImage(1L, "second.jpg", "WATERMARK_RENDER_FAILED");
        List<UUID> requestOrder = List.of(
                second.getPromptImageId().id(),
                first.getPromptImageId().id()
        );
        when(loadPromptImagePort.listByIds(requestOrder)).thenReturn(List.of(first, second));
        when(presignPromptImageDownloadPort.presignGet(first.getWatermarkedObjectKey()))
                .thenReturn("https://storage.example.com/first.jpg");

        PromptImageStatusesInfo info =
                service.getStatuses(new GetPromptImageStatusesQuery(1L, requestOrder));

        assertThat(info.images())
                .extracting("imageId")
                .containsExactly(second.getPromptImageId().id(), first.getPromptImageId().id());
        assertThat(info.images())
                .extracting("status")
                .containsExactly(PromptImageStatus.FAILED, PromptImageStatus.READY);
        assertThat(info.images().getFirst().failureCode()).isEqualTo("WATERMARK_RENDER_FAILED");
        assertThat(info.images().getFirst().imageUrl()).isNull();
        assertThat(info.images().get(1).failureCode()).isNull();
        assertThat(info.images().get(1).imageUrl()).isEqualTo("https://storage.example.com/first.jpg");
        verify(presignPromptImageDownloadPort).presignGet(first.getWatermarkedObjectKey());
    }

    @DisplayName("중복된 imageId는 DB 조회 전에 거절한다")
    @Test
    void duplicateImageIdsAreRejected() {
        UUID imageId = UUID.randomUUID();

        assertThatThrownBy(() -> service.getStatuses(new GetPromptImageStatusesQuery(
                1L,
                List.of(imageId, imageId)
        )))
                .isInstanceOf(PromptDomainException.class)
                .extracting("baseCode")
                .isEqualTo(PromptErrorCode.DUPLICATE_IMAGE_ID);
    }

    @DisplayName("요청한 이미지 중 하나라도 없으면 전체 요청을 실패한다")
    @Test
    void missingImageFailsWholeRequest() {
        PromptImage image = readyImage(1L, "first.jpg");
        UUID missingId = UUID.randomUUID();
        List<UUID> imageIds = List.of(image.getPromptImageId().id(), missingId);
        when(loadPromptImagePort.listByIds(imageIds)).thenReturn(List.of(image));

        assertThatThrownBy(() -> service.getStatuses(new GetPromptImageStatusesQuery(1L, imageIds)))
                .isInstanceOf(PromptDomainException.class)
                .extracting("baseCode")
                .isEqualTo(PromptErrorCode.IMAGE_NOT_FOUND);
    }

    @DisplayName("요청한 이미지 중 하나라도 본인 소유가 아니면 전체 요청을 실패한다")
    @Test
    void notOwnedImageFailsWholeRequest() {
        PromptImage ownImage = readyImage(1L, "first.jpg");
        PromptImage otherImage = readyImage(2L, "second.jpg");
        List<UUID> imageIds = List.of(
                ownImage.getPromptImageId().id(),
                otherImage.getPromptImageId().id()
        );
        when(loadPromptImagePort.listByIds(imageIds)).thenReturn(List.of(ownImage, otherImage));

        assertThatThrownBy(() -> service.getStatuses(new GetPromptImageStatusesQuery(1L, imageIds)))
                .isInstanceOf(PromptDomainException.class)
                .extracting("baseCode")
                .isEqualTo(PromptErrorCode.IMAGE_NOT_OWNED);
    }

    @DisplayName("이미지 상태 조회는 1~10개만 허용한다")
    @Test
    void imageCountMustBeBetweenOneAndTen() {
        List<UUID> imageIds = java.util.stream.Stream.generate(UUID::randomUUID)
                .limit(11)
                .toList();

        assertThatThrownBy(() -> service.getStatuses(new GetPromptImageStatusesQuery(1L, imageIds)))
                .isInstanceOf(PromptDomainException.class)
                .extracting("baseCode")
                .isEqualTo(PromptErrorCode.INVALID_IMAGE_STATUS_QUERY_COUNT);
    }

    private PromptImage readyImage(Long uploaderId, String fileName) {
        PromptImage image = uploadingImage(uploaderId, fileName);
        image.completeUpload("\"etag\"", Instant.parse("2026-07-26T01:00:00Z"));
        image.startProcessing();
        image.completeProcessing("watermarked/" + uploaderId + "/" + image.getPromptImageId().id() + ".jpg", 1);
        return image;
    }

    private PromptImage failedImage(Long uploaderId, String fileName, String failureCode) {
        PromptImage image = uploadingImage(uploaderId, fileName);
        image.completeUpload("\"etag\"", Instant.parse("2026-07-26T01:00:00Z"));
        image.startProcessing();
        image.failProcessing(failureCode);
        return image;
    }

    private PromptImage uploadingImage(Long uploaderId, String fileName) {
        UUID imageId = UUID.randomUUID();
        return PromptImage.prepareUpload(
                imageId,
                uploaderId,
                "originals/" + uploaderId + "/" + imageId + ".jpg",
                fileName,
                PromptImageContentType.JPEG,
                1_024,
                1_920,
                1_080
        );
    }
}
