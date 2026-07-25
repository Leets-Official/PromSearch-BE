package com.promsearch.prompt.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.promsearch.prompt.domain.enums.PromptImageContentType;
import com.promsearch.prompt.domain.enums.PromptImageStatus;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PromptImageTest {

    @DisplayName("이미지 업로드 준비 시 UUID와 원본 메타데이터를 보관한다")
    @Test
    void prepareUploadCreatesUploadingAsset() {
        UUID imageId = UUID.randomUUID();

        PromptImage image = PromptImage.prepareUpload(
                imageId,
                1L,
                "originals/1/" + imageId + ".jpg",
                " result.jpg ",
                PromptImageContentType.JPEG,
                5_242_880L,
                1_920,
                1_080
        );

        assertThat(image.getPromptImageId().id()).isEqualTo(imageId);
        assertThat(image.getUploaderId()).isEqualTo(1L);
        assertThat(image.getPromptId()).isNull();
        assertThat(image.getOriginalFileName()).isEqualTo("result.jpg");
        assertThat(image.getStatus()).isEqualTo(PromptImageStatus.UPLOADING);
        assertThat(image.getWatermarkedObjectKey()).isNull();
        assertThat(image.getSortOrder()).isNull();
        assertThat(image.isThumbnail()).isFalse();
    }

    @DisplayName("JPEG와 PNG MIME 타입만 지원한다")
    @Test
    void contentTypeSupportsJpegAndPngOnly() {
        assertThat(PromptImageContentType.fromMimeType(" IMAGE/JPEG "))
                .isEqualTo(PromptImageContentType.JPEG);
        assertThat(PromptImageContentType.fromMimeType("image/png"))
                .isEqualTo(PromptImageContentType.PNG);

        assertPromptError(
                () -> PromptImageContentType.fromMimeType("image/webp"),
                PromptErrorCode.UNSUPPORTED_IMAGE_CONTENT_TYPE
        );
    }

    @DisplayName("파일 크기와 이미지 크기 정책을 검증한다")
    @Test
    void prepareUploadValidatesImageMetadata() {
        assertPromptError(
                () -> prepareImage(PromptImage.MAX_FILE_SIZE + 1, 1_920, 1_080),
                PromptErrorCode.INVALID_IMAGE_FILE_SIZE
        );
        assertPromptError(
                () -> prepareImage(1_024, PromptImage.MAX_DIMENSION + 1, 1_080),
                PromptErrorCode.INVALID_IMAGE_DIMENSIONS
        );
        assertPromptError(
                () -> prepareImage(1_024, 8_000, 8_000),
                PromptErrorCode.INVALID_IMAGE_DIMENSIONS
        );
    }

    @DisplayName("공개 URL과 경로가 포함된 파일명은 스토리지 메타데이터로 사용할 수 없다")
    @Test
    void storageMetadataRejectsUrlAndPathFileName() {
        UUID imageId = UUID.randomUUID();

        assertPromptError(
                () -> PromptImage.prepareUpload(
                        imageId,
                        1L,
                        "https://bucket.example.com/original.jpg",
                        "result.jpg",
                        PromptImageContentType.JPEG,
                        1_024,
                        1_920,
                        1_080
                ),
                PromptErrorCode.INVALID_IMAGE_OBJECT_KEY
        );
        assertPromptError(
                () -> PromptImage.prepareUpload(
                        imageId,
                        1L,
                        "originals/1/" + imageId + ".jpg",
                        "../result.jpg",
                        PromptImageContentType.JPEG,
                        1_024,
                        1_920,
                        1_080
                ),
                PromptErrorCode.INVALID_IMAGE_FILE_NAME
        );
    }

    @DisplayName("워터마크 처리가 완료된 본인 이미지만 프롬프트에 연결한다")
    @Test
    void readyImageCanBeAttachedToPrompt() {
        PromptImage image = prepareImage(1_024, 1_920, 1_080);

        image.startProcessing();
        image.completeProcessing("watermarked/1/result.jpg", 1);
        image.attachToPrompt(10L, 1L, 0, true);

        assertThat(image.getStatus()).isEqualTo(PromptImageStatus.READY);
        assertThat(image.getWatermarkedObjectKey()).isEqualTo("watermarked/1/result.jpg");
        assertThat(image.getProcessingVersion()).isEqualTo(1);
        assertThat(image.getPromptId()).isEqualTo(10L);
        assertThat(image.getSortOrder()).isZero();
        assertThat(image.isThumbnail()).isTrue();
    }

    @DisplayName("READY 이전 이미지는 프롬프트에 연결할 수 없다")
    @Test
    void uploadingImageCannotBeAttached() {
        PromptImage image = prepareImage(1_024, 1_920, 1_080);

        assertPromptError(
                () -> image.attachToPrompt(10L, 1L, 0, false),
                PromptErrorCode.IMAGE_NOT_READY
        );
    }

    @DisplayName("다른 사용자가 업로드한 이미지는 연결할 수 없다")
    @Test
    void imageOwnerIsVerifiedWhenAttaching() {
        PromptImage image = readyImage();

        assertPromptError(
                () -> image.attachToPrompt(10L, 2L, 0, false),
                PromptErrorCode.IMAGE_NOT_OWNED
        );
    }

    @DisplayName("이미 연결된 이미지를 다른 프롬프트에 다시 연결할 수 없다")
    @Test
    void imageCannotBeAttachedTwice() {
        PromptImage image = readyImage();
        image.attachToPrompt(10L, 1L, 0, false);

        assertPromptError(
                () -> image.attachToPrompt(11L, 1L, 1, false),
                PromptErrorCode.IMAGE_ALREADY_ATTACHED
        );
    }

    @DisplayName("처리에 실패한 이미지는 실패 원인을 지우고 다시 처리할 수 있다")
    @Test
    void failedImageCanRestartProcessing() {
        PromptImage image = prepareImage(1_024, 1_920, 1_080);
        image.startProcessing();
        image.failProcessing("WATERMARK_RENDER_FAILED");

        assertThat(image.getStatus()).isEqualTo(PromptImageStatus.FAILED);
        assertThat(image.getFailureCode()).isEqualTo("WATERMARK_RENDER_FAILED");

        image.startProcessing();

        assertThat(image.getStatus()).isEqualTo(PromptImageStatus.PROCESSING);
        assertThat(image.getFailureCode()).isNull();
    }

    @DisplayName("허용되지 않은 처리 상태 전이는 거절한다")
    @Test
    void invalidProcessingTransitionIsRejected() {
        PromptImage image = prepareImage(1_024, 1_920, 1_080);

        assertPromptError(
                () -> image.completeProcessing("watermarked/1/result.jpg", 1),
                PromptErrorCode.INVALID_IMAGE_STATUS_TRANSITION
        );
    }

    @DisplayName("워터마크 결과는 원본과 다른 Object Key에 저장한다")
    @Test
    void watermarkedObjectUsesDifferentKey() {
        PromptImage image = prepareImage(1_024, 1_920, 1_080);
        image.startProcessing();

        assertPromptError(
                () -> image.completeProcessing(image.getOriginalObjectKey(), 1),
                PromptErrorCode.INVALID_IMAGE_OBJECT_KEY
        );
    }

    private PromptImage readyImage() {
        PromptImage image = prepareImage(1_024, 1_920, 1_080);
        image.startProcessing();
        image.completeProcessing("watermarked/1/" + image.getPromptImageId().id() + ".jpg", 1);
        return image;
    }

    private PromptImage prepareImage(long fileSize, int width, int height) {
        UUID imageId = UUID.randomUUID();
        return PromptImage.prepareUpload(
                imageId,
                1L,
                "originals/1/" + imageId + ".jpg",
                "result.jpg",
                PromptImageContentType.JPEG,
                fileSize,
                width,
                height
        );
    }

    private void assertPromptError(Runnable action, PromptErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(PromptDomainException.class)
                .extracting("baseCode")
                .isEqualTo(errorCode);
    }
}
