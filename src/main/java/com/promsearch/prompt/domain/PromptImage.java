package com.promsearch.prompt.domain;

import com.promsearch.prompt.domain.enums.PromptImageContentType;
import com.promsearch.prompt.domain.enums.PromptImageStatus;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

/**
 * 프롬프트에 연결되기 전부터 존재하는 이미지 자산이다.
 *
 * <p>외부 공개 URL은 만료되거나 배포 환경마다 달라질 수 있으므로 도메인에는 저장하지 않는다.
 * 원본과 워터마크 결과의 Object Key만 보관하고, 조회 시 스토리지 어댑터가 URL로 변환한다.</p>
 */
@Getter
public class PromptImage {

    public static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    public static final int MAX_DIMENSION = 8_192;
    public static final long MAX_PIXEL_COUNT = 40_000_000L;

    private static final int MAX_ORIGINAL_FILE_NAME_LENGTH = 255;
    private static final int MAX_OBJECT_KEY_LENGTH = 1_024;
    private static final int MAX_FAILURE_CODE_LENGTH = 100;

    private final PromptImageId promptImageId;
    private final Long uploaderId;
    private Long promptId;
    private final String originalObjectKey;
    private String watermarkedObjectKey;
    private final String originalFileName;
    private final PromptImageContentType contentType;
    /*
     * 업로드 준비 요청에서 전달된 예상 메타데이터다.
     * Presigned URL 발급 시에는 클라이언트 값을 신뢰할 수 없으므로, 업로드 완료 단계에서
     * HeadObject와 실제 이미지 디코딩 결과를 다시 검증해야 한다.
     */
    private final long fileSize;
    private final int width;
    private final int height;
    private PromptImageStatus status;
    private String etag;
    private Instant uploadedAt;
    /** 워터마크 알고리즘 버전이며 JPA의 낙관적 잠금 버전과는 별개의 값이다. */
    private int processingVersion;
    private String failureCode;
    private Integer sortOrder;
    private boolean thumbnail;
    private final Instant createdAt;
    private Instant updatedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private PromptImage(
            PromptImageId promptImageId,
            Long uploaderId,
            Long promptId,
            String originalObjectKey,
            String watermarkedObjectKey,
            String originalFileName,
            PromptImageContentType contentType,
            long fileSize,
            int width,
            int height,
            PromptImageStatus status,
            String etag,
            Instant uploadedAt,
            int processingVersion,
            String failureCode,
            Integer sortOrder,
            boolean thumbnail,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.promptImageId = promptImageId;
        this.uploaderId = uploaderId;
        this.promptId = promptId;
        this.originalObjectKey = originalObjectKey;
        this.watermarkedObjectKey = watermarkedObjectKey;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.width = width;
        this.height = height;
        this.status = status;
        this.etag = etag;
        this.uploadedAt = uploadedAt;
        this.processingVersion = processingVersion;
        this.failureCode = failureCode;
        this.sortOrder = sortOrder;
        this.thumbnail = thumbnail;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** 예상 메타데이터 검증 및 UPLOADING 이미지 생성 */
    public static PromptImage prepareUpload(
            UUID imageId,
            Long uploaderId,
            String originalObjectKey,
            String originalFileName,
            PromptImageContentType contentType,
            long fileSize,
            int width,
            int height
    ) {
        PromptImageId promptImageId = new PromptImageId(imageId);
        validateUploaderId(uploaderId);
        validateObjectKey(originalObjectKey);
        validateOriginalFileName(originalFileName);
        validateMetadata(contentType, fileSize, width, height);

        Instant now = Instant.now();
        return PromptImage.builder()
                .promptImageId(promptImageId)
                .uploaderId(uploaderId)
                .originalObjectKey(originalObjectKey.trim())
                .originalFileName(originalFileName.trim())
                .contentType(contentType)
                .fileSize(fileSize)
                .width(width)
                .height(height)
                .status(PromptImageStatus.UPLOADING)
                .processingVersion(0)
                .thumbnail(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /** 영속 상태 불변식 검증 및 이미지 자산 복원 */
    public static PromptImage reconstruct(
            PromptImageId promptImageId,
            Long uploaderId,
            Long promptId,
            String originalObjectKey,
            String watermarkedObjectKey,
            String originalFileName,
            PromptImageContentType contentType,
            long fileSize,
            int width,
            int height,
            PromptImageStatus status,
            String etag,
            Instant uploadedAt,
            int processingVersion,
            String failureCode,
            Integer sortOrder,
            boolean thumbnail,
            Instant createdAt,
            Instant updatedAt
    ) {
        if (promptImageId == null) {
            throw new PromptDomainException(PromptErrorCode.INVALID_ID);
        }
        validateUploaderId(uploaderId);
        validateObjectKey(originalObjectKey);
        validateOriginalFileName(originalFileName);
        validateMetadata(contentType, fileSize, width, height);
        validateReconstructedState(
                promptId,
                originalObjectKey,
                watermarkedObjectKey,
                status,
                etag,
                uploadedAt,
                processingVersion,
                failureCode,
                sortOrder,
                thumbnail
        );
        if (createdAt == null || updatedAt == null) {
            throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_METADATA);
        }

        return PromptImage.builder()
                .promptImageId(promptImageId)
                .uploaderId(uploaderId)
                .promptId(promptId)
                .originalObjectKey(originalObjectKey.trim())
                .watermarkedObjectKey(trimToNull(watermarkedObjectKey))
                .originalFileName(originalFileName.trim())
                .contentType(contentType)
                .fileSize(fileSize)
                .width(width)
                .height(height)
                .status(status)
                .etag(trimToNull(etag))
                .uploadedAt(uploadedAt)
                .processingVersion(processingVersion)
                .failureCode(trimToNull(failureCode))
                .sortOrder(sortOrder)
                .thumbnail(thumbnail)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    /** 업로드 증거 기록 및 UPLOADED 상태 전환 */
    public void completeUpload(String etag, Instant uploadedAt) {
        requireStatus(PromptImageStatus.UPLOADING);
        validateUploadMetadata(etag, uploadedAt);

        this.etag = etag.trim();
        this.uploadedAt = uploadedAt;
        this.status = PromptImageStatus.UPLOADED;
        touch();
    }

    /** 워터마크 처리 시작 및 실패 처리 재시도 */
    public void startProcessing() {
        if (status != PromptImageStatus.UPLOADED && status != PromptImageStatus.FAILED) {
            throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_STATUS_TRANSITION);
        }

        status = PromptImageStatus.PROCESSING;
        watermarkedObjectKey = null;
        failureCode = null;
        touch();
    }

    /** 워터마크 결과·정책 버전 기록 및 READY 상태 전환 */
    public void completeProcessing(String watermarkedObjectKey, int processingVersion) {
        /*
         * 이 메서드를 호출하기 전에 Worker가 실제 MIME 타입, 파일 크기, 이미지 크기를 검증해야 한다.
         * 추후 실제 메타데이터를 별도로 보존해야 한다면 completeProcessing 입력과 DB 컬럼을 확장한다.
         */
        requireStatus(PromptImageStatus.PROCESSING);
        validateObjectKey(watermarkedObjectKey);
        if (originalObjectKey.equals(watermarkedObjectKey.trim())) {
            throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_OBJECT_KEY);
        }
        if (processingVersion <= 0) {
            throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_PROCESSING_VERSION);
        }

        this.watermarkedObjectKey = watermarkedObjectKey.trim();
        this.processingVersion = processingVersion;
        this.failureCode = null;
        this.status = PromptImageStatus.READY;
        touch();
    }

    /** 처리 실패 코드 기록 및 FAILED 상태 전환 */
    public void failProcessing(String failureCode) {
        requireStatus(PromptImageStatus.PROCESSING);
        if (failureCode == null || failureCode.isBlank() || failureCode.length() > MAX_FAILURE_CODE_LENGTH) {
            throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_FAILURE_CODE);
        }

        this.failureCode = failureCode.trim();
        this.status = PromptImageStatus.FAILED;
        touch();
    }

    /** 소유권·READY 상태·중복 연결 검증 및 프롬프트 연결 */
    public void attachToPrompt(Long promptId, Long requesterId, int sortOrder, boolean thumbnail) {
        attachToPrompt(promptId, requesterId, sortOrder, thumbnail, null);
    }

    /** 현재 사용자의 초안에 연결된 이미지는 게시 생성 시 새 프롬프트로 옮길 수 있다. */
    public void attachToPrompt(
            Long promptId,
            Long requesterId,
            int sortOrder,
            boolean thumbnail,
            Long reusableDraftPromptId
    ) {
        /*
         * 생성 API에서는 여러 이미지를 한 트랜잭션에서 연결하고, JPA lock_version 충돌을 처리해야 한다.
         * 그래야 동일 이미지를 동시에 두 프롬프트에 연결하는 경쟁 상태를 막을 수 있다.
         */
        validatePromptId(promptId);
        validateUploaderId(requesterId);
        if (!Objects.equals(uploaderId, requesterId)) {
            throw new PromptDomainException(PromptErrorCode.IMAGE_NOT_OWNED);
        }
        if (status != PromptImageStatus.READY) {
            throw new PromptDomainException(PromptErrorCode.IMAGE_NOT_READY);
        }
        if (this.promptId != null && !Objects.equals(this.promptId, reusableDraftPromptId)) {
            throw new PromptDomainException(PromptErrorCode.IMAGE_ALREADY_ATTACHED);
        }
        if (sortOrder < 0) {
            throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_ORDER);
        }

        this.promptId = promptId;
        this.sortOrder = sortOrder;
        this.thumbnail = thumbnail;
        touch();
    }

    /** 초안 저장용 이미지 연결. 같은 초안에 연결되어 있던 이미지는 전체 교체 중 재정렬할 수 있다. */
    public void attachToDraft(Long draftPromptId, Long requesterId, int sortOrder, boolean thumbnail) {
        validatePromptId(draftPromptId);
        validateUploaderId(requesterId);
        if (!Objects.equals(uploaderId, requesterId)) {
            throw new PromptDomainException(PromptErrorCode.IMAGE_NOT_OWNED);
        }
        if (status != PromptImageStatus.READY) {
            throw new PromptDomainException(PromptErrorCode.IMAGE_NOT_READY);
        }
        if (promptId != null && !Objects.equals(promptId, draftPromptId)) {
            throw new PromptDomainException(PromptErrorCode.IMAGE_ALREADY_ATTACHED);
        }
        if (sortOrder < 0) {
            throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_ORDER);
        }

        promptId = draftPromptId;
        this.sortOrder = sortOrder;
        this.thumbnail = thumbnail;
        touch();
    }

    /** 초안에서 제외되거나 초안이 삭제된 이미지를 다시 미연결 상태로 되돌린다. */
    public void detachFromPrompt(Long requesterId, Long expectedPromptId) {
        validateUploaderId(requesterId);
        validatePromptId(expectedPromptId);
        if (!Objects.equals(uploaderId, requesterId)) {
            throw new PromptDomainException(PromptErrorCode.IMAGE_NOT_OWNED);
        }
        if (!Objects.equals(promptId, expectedPromptId)) {
            throw new PromptDomainException(PromptErrorCode.IMAGE_ALREADY_ATTACHED);
        }

        promptId = null;
        sortOrder = null;
        thumbnail = false;
        touch();
    }

    /** 이미지 업로더 일치 여부 반환 */
    public boolean isOwnedBy(Long userId) {
        return userId != null && Objects.equals(uploaderId, userId);
    }

    /** 프롬프트 연결 가능 상태 반환 */
    public boolean isReady() {
        return status == PromptImageStatus.READY;
    }

    /** 업로드 완료 요청 멱등 처리 여부 반환 */
    public boolean isUploadCompleted() {
        return status != PromptImageStatus.UPLOADING && etag != null && uploadedAt != null;
    }

    /** 도메인 동작이 허용되는 선행 상태인지 확인 */
    private void requireStatus(PromptImageStatus requiredStatus) {
        if (status != requiredStatus) {
            throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_STATUS_TRANSITION);
        }
    }

    /** 명시적인 상태 변경 시각을 현재 시각으로 갱신 */
    private void touch() {
        updatedAt = Instant.now();
    }

    /** 이미지 소유자를 나타내는 양수 사용자 식별자인지 확인 */
    private static void validateUploaderId(Long uploaderId) {
        if (uploaderId == null || uploaderId <= 0) {
            throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_UPLOADER_ID);
        }
    }

    /** 연결 대상 프롬프트가 유효한 양수 식별자인지 확인 */
    private static void validatePromptId(Long promptId) {
        if (promptId == null || promptId <= 0) {
            throw new PromptDomainException(PromptErrorCode.INVALID_ID);
        }
    }

    /** S3 URL이 아닌 안전한 상대 Object Key 형식인지 확인 */
    private static void validateObjectKey(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_OBJECT_KEY);
        }

        String normalizedObjectKey = objectKey.trim();
        if (normalizedObjectKey.length() > MAX_OBJECT_KEY_LENGTH
                || normalizedObjectKey.startsWith("/")
                || normalizedObjectKey.startsWith("\\")
                || normalizedObjectKey.contains("://")
                || normalizedObjectKey.chars().anyMatch(Character::isISOControl)) {
            throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_OBJECT_KEY);
        }
    }

    /** 경로 문자가 포함되지 않은 표시용 원본 파일명인지 확인 */
    private static void validateOriginalFileName(String originalFileName) {
        if (originalFileName == null
                || originalFileName.isBlank()
                || originalFileName.length() > MAX_ORIGINAL_FILE_NAME_LENGTH
                || originalFileName.contains("/")
                || originalFileName.contains("\\")
                || originalFileName.chars().anyMatch(Character::isISOControl)) {
            throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_FILE_NAME);
        }
    }

    /** 허용 형식·파일 크기·해상도·전체 픽셀 수 정책을 함께 검증 */
    private static void validateMetadata(
            PromptImageContentType contentType,
            long fileSize,
            int width,
            int height
    ) {
        if (contentType == null) {
            throw new PromptDomainException(PromptErrorCode.UNSUPPORTED_IMAGE_CONTENT_TYPE);
        }
        if (fileSize <= 0 || fileSize > MAX_FILE_SIZE) {
            throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_FILE_SIZE);
        }
        if (width <= 0 || height <= 0
                || width > MAX_DIMENSION
                || height > MAX_DIMENSION
                || (long) width * height > MAX_PIXEL_COUNT) {
            throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_DIMENSIONS);
        }
    }

    /** DB에서 복원한 필드 조합이 이미지 상태별 불변식을 만족하는지 검증 */
    private static void validateReconstructedState(
            Long promptId,
            String originalObjectKey,
            String watermarkedObjectKey,
            PromptImageStatus status,
            String etag,
            Instant uploadedAt,
            int processingVersion,
            String failureCode,
            Integer sortOrder,
            boolean thumbnail
    ) {
        if (status == null || processingVersion < 0) {
            throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_METADATA);
        }

        String normalizedWatermarkedKey = trimToNull(watermarkedObjectKey);
        String normalizedFailureCode = trimToNull(failureCode);
        String normalizedEtag = trimToNull(etag);

        if ((normalizedEtag == null) != (uploadedAt == null)) {
            throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_METADATA);
        }
        if (normalizedEtag != null) {
            validateUploadMetadata(normalizedEtag, uploadedAt);
        }
        if (status == PromptImageStatus.UPLOADING && normalizedEtag != null) {
            throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_METADATA);
        }
        if (status == PromptImageStatus.UPLOADED
                && (normalizedEtag == null
                || normalizedWatermarkedKey != null
                || normalizedFailureCode != null
                || processingVersion != 0)) {
            throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_METADATA);
        }

        if (status == PromptImageStatus.READY
                && (normalizedWatermarkedKey == null
                || normalizedWatermarkedKey.equals(originalObjectKey.trim())
                || processingVersion <= 0
                || normalizedFailureCode != null)) {
            throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_METADATA);
        }
        if (normalizedWatermarkedKey != null) {
            validateObjectKey(normalizedWatermarkedKey);
        }
        if (status == PromptImageStatus.FAILED
                && (normalizedFailureCode == null
                || normalizedFailureCode.length() > MAX_FAILURE_CODE_LENGTH
                || normalizedWatermarkedKey != null)) {
            throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_METADATA);
        }
        if ((status == PromptImageStatus.UPLOADING
                || status == PromptImageStatus.UPLOADED
                || status == PromptImageStatus.PROCESSING)
                && (normalizedWatermarkedKey != null || normalizedFailureCode != null || processingVersion != 0)) {
            throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_METADATA);
        }

        if (promptId == null) {
            if (sortOrder != null || thumbnail) {
                throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_METADATA);
            }
            return;
        }

        validatePromptId(promptId);
        if (status != PromptImageStatus.READY || sortOrder == null || sortOrder < 0) {
            throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_METADATA);
        }
    }

    /** 선택 문자열의 앞뒤 공백을 제거하고 빈 값은 null로 정규화 */
    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /** 업로드 완료를 증명하는 ETag와 S3 수정 시각의 일관성을 검증 */
    private static void validateUploadMetadata(String etag, Instant uploadedAt) {
        if (etag == null
                || etag.isBlank()
                || etag.length() > 255
                || etag.chars().anyMatch(Character::isISOControl)
                || uploadedAt == null) {
            throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_UPLOAD_METADATA);
        }
    }

    public record PromptImageId(UUID id) {
        public PromptImageId {
            if (id == null) {
                throw new PromptDomainException(PromptErrorCode.INVALID_ID);
            }
        }
    }
}
