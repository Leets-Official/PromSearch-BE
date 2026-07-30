package com.promsearch.prompt.infrastructure.storage.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.promsearch.prompt.application.port.out.storage.LoadPromptImageObjectMetadataPort.StoredObjectMetadata;
import com.promsearch.prompt.application.port.out.storage.PresignPromptImageUploadPort.PresignedUpload;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

class S3PromptImageStorageAdapterTest {

    private S3Client s3Client;
    private S3Presigner s3Presigner;
    private S3PromptImageStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        s3Presigner = S3Presigner.builder()
                .region(Region.AP_NORTHEAST_2)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test-access-key", "test-secret-key")
                ))
                .build();
        adapter = new S3PromptImageStorageAdapter(
                s3Client,
                s3Presigner,
                new S3StorageProperties(
                        "promsearch-test-bucket",
                        "ap-northeast-2",
                        "prompt-images/original",
                        Duration.ofMinutes(10)
                )
        );
    }

    @AfterEach
    void tearDown() {
        s3Presigner.close();
    }

    @DisplayName("Presigned PUT URL은 Content-Type 헤더를 서명에 포함한다")
    @Test
    void presignPutSignsContentType() {
        PresignedUpload result = adapter.presignPut(
                "prompt-images/original/1/image.jpg",
                "image/jpeg"
        );

        String decodedQuery = URLDecoder.decode(
                result.uploadUrl().getRawQuery(),
                StandardCharsets.UTF_8
        );
        assertThat(decodedQuery).contains("X-Amz-Signature=");
        assertThat(decodedQuery).contains("X-Amz-SignedHeaders=content-type;host");
        assertThat(result.expiresAt()).isAfter(Instant.now().plus(Duration.ofMinutes(9)));
    }

    @DisplayName("HeadObject 응답을 애플리케이션 스토리지 메타데이터로 변환한다")
    @Test
    void getMetadataMapsHeadObjectResponse() {
        Instant lastModified = Instant.parse("2026-07-26T01:00:00Z");
        when(s3Client.headObject(org.mockito.ArgumentMatchers.any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder()
                        .contentLength(1_024L)
                        .contentType("image/jpeg")
                        .eTag("\"etag\"")
                        .lastModified(lastModified)
                        .build());

        StoredObjectMetadata metadata =
                adapter.getMetadata("prompt-images/original/1/image.jpg");

        assertThat(metadata.contentLength()).isEqualTo(1_024L);
        assertThat(metadata.contentType()).isEqualTo("image/jpeg");
        assertThat(metadata.etag()).isEqualTo("\"etag\"");
        assertThat(metadata.lastModified()).isEqualTo(lastModified);
    }

    @DisplayName("HeadObject 404는 업로드 객체 없음 오류로 변환한다")
    @Test
    void getMetadataTranslatesNotFound() {
        when(s3Client.headObject(org.mockito.ArgumentMatchers.any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(404).message("Not Found").build());

        assertThatThrownBy(() -> adapter.getMetadata("prompt-images/original/1/missing.jpg"))
                .isInstanceOf(PromptDomainException.class)
                .extracting("baseCode")
                .isEqualTo(PromptErrorCode.IMAGE_UPLOAD_NOT_FOUND);
    }
}
