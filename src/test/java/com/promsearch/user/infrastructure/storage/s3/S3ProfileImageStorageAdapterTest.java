package com.promsearch.user.infrastructure.storage.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.promsearch.global.infrastructure.storage.s3.S3StorageProperties;
import com.promsearch.user.application.port.out.profileimage.LoadProfileImageObjectMetadataPort.StoredObjectMetadata;
import com.promsearch.user.application.port.out.profileimage.PresignProfileImageUploadPort.PresignedUpload;
import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

class S3ProfileImageStorageAdapterTest {

    private S3Client s3Client;
    private S3Presigner s3Presigner;
    private S3ProfileImageStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        s3Presigner = S3Presigner.builder()
                .region(Region.AP_NORTHEAST_2)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test-access-key", "test-secret-key")
                ))
                .build();
        adapter = new S3ProfileImageStorageAdapter(
                s3Client,
                s3Presigner,
                new S3StorageProperties(
                        "promsearch-test-bucket",
                        "ap-northeast-2",
                        "prompt-images/original",
                        "prompt-images/watermarked",
                        "profiles",
                        "https://cdn.example.com/",
                        Duration.ofMinutes(10)
                )
        );
    }

    @AfterEach
    void tearDown() {
        s3Presigner.close();
    }

    @Test
    void presignPutSignsContentType() {
        PresignedUpload result = adapter.presignPut(
                "profiles/12/123e4567-e89b-12d3-a456-426614174000.jpg",
                "image/jpeg",
                1_024L
        );

        String query = URLDecoder.decode(result.uploadUrl().getRawQuery(), StandardCharsets.UTF_8);
        assertThat(query).contains("X-Amz-SignedHeaders=content-length;content-type;host;if-none-match");
    }

    @Test
    void mapsHeadObjectAndResolvesConfiguredPublicUrl() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder()
                        .contentLength(1_024L)
                        .contentType("image/jpeg")
                        .build());
        String objectKey = "profiles/12/123e4567-e89b-12d3-a456-426614174000.jpg";

        StoredObjectMetadata metadata = adapter.getMetadata(objectKey);

        assertThat(metadata.contentLength()).isEqualTo(1_024L);
        assertThat(metadata.contentType()).isEqualTo("image/jpeg");
        assertThat(adapter.resolve(objectKey))
                .isEqualTo("https://cdn.example.com/" + objectKey);
    }

    @Test
    void translatesMissingUploadAndDeletesIdempotently() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(404).message("Not Found").build());
        String objectKey = "profiles/12/123e4567-e89b-12d3-a456-426614174000.jpg";

        assertThatThrownBy(() -> adapter.getMetadata(objectKey))
                .isInstanceOf(UserDomainException.class)
                .extracting("baseCode")
                .isEqualTo(UserErrorCode.PROFILE_IMAGE_UPLOAD_NOT_FOUND);

        adapter.delete(objectKey);
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }
}
