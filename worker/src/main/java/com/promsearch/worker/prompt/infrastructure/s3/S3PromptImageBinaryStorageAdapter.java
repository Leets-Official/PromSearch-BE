package com.promsearch.worker.prompt.infrastructure.s3;

import com.promsearch.prompt.application.port.out.storage.LoadPromptImageBinaryPort;
import com.promsearch.prompt.application.port.out.storage.SavePromptImageBinaryPort;
import com.promsearch.prompt.domain.PromptImage;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import com.promsearch.common.infrastructure.storage.s3.S3ObjectStorageProperties;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/** Worker 전용 S3 원본 다운로드·결과 업로드 어댑터 */
@Component
@Slf4j
@RequiredArgsConstructor
public class S3PromptImageBinaryStorageAdapter
        implements LoadPromptImageBinaryPort, SavePromptImageBinaryPort {

    private final S3Client s3Client;
    private final S3ObjectStorageProperties properties;

    /** 응답 스트림을 최대 허용 크기까지만 읽어 메모리 과다 사용 방지 */
    @Override
    public StoredImage load(String objectKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .build();

        try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request)) {
            if (response.response().contentLength() != null
                    && response.response().contentLength() > PromptImage.MAX_FILE_SIZE) {
                throw invalidSource(objectKey);
            }
            byte[] bytes = response.readNBytes((int) PromptImage.MAX_FILE_SIZE + 1);
            if (bytes.length > PromptImage.MAX_FILE_SIZE) {
                throw invalidSource(objectKey);
            }
            String contentType = response.response().contentType();
            if (contentType == null || contentType.isBlank()) {
                throw invalidSource(objectKey);
            }
            return new StoredImage(bytes, contentType);
        } catch (PromptDomainException exception) {
            throw exception;
        } catch (IOException | SdkException exception) {
            log.warn("prompt_image_worker_s3_download_failed objectKey={} errorType={}",
                    objectKey, exception.getClass().getSimpleName());
            throw new PromptDomainException(
                    PromptErrorCode.IMAGE_ORIGINAL_DOWNLOAD_FAILED,
                    "S3 원본 이미지를 다운로드할 수 없습니다.",
                    exception
            );
        }
    }

    /** 워터마크 결과를 메시지의 결정적 Object Key에 저장 */
    @Override
    public void save(String objectKey, String contentType, byte[] bytes) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .contentType(contentType)
                .build();
        try {
            s3Client.putObject(request, RequestBody.fromBytes(bytes));
        } catch (SdkException exception) {
            log.warn("prompt_image_worker_s3_upload_failed objectKey={} errorType={}",
                    objectKey, exception.getClass().getSimpleName());
            throw new PromptDomainException(
                    PromptErrorCode.IMAGE_WATERMARK_UPLOAD_FAILED,
                    "S3에 워터마크 결과를 업로드할 수 없습니다.",
                    exception
            );
        }
    }

    /** S3 원본의 형식·크기 불일치를 Worker가 처리할 수 있는 도메인 오류로 변환 */
    private PromptDomainException invalidSource(String objectKey) {
        log.warn("prompt_image_worker_s3_source_too_large objectKey={}", objectKey);
        return new PromptDomainException(
                PromptErrorCode.INVALID_IMAGE_SOURCE,
                "S3 원본 이미지가 허용 크기를 초과했습니다."
        );
    }
}
