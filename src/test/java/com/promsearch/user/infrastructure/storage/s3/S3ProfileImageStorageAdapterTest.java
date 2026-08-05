package com.promsearch.user.infrastructure.storage.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.promsearch.common.infrastructure.storage.s3.S3ObjectStorageOperations;
import com.promsearch.common.infrastructure.storage.s3.S3ObjectStorageProperties;
import com.promsearch.user.application.port.out.profileimage.ProfileImageStoragePort.PresignedUpload;
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
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

class S3ProfileImageStorageAdapterTest {

    private S3Presigner s3Presigner;
    private S3ProfileImageStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        S3Client s3Client = mock(S3Client.class);
        s3Presigner = S3Presigner.builder()
                .region(Region.AP_NORTHEAST_2)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test-access-key", "test-secret-key")
                ))
                .build();
        adapter = new S3ProfileImageStorageAdapter(new S3ObjectStorageOperations(
                s3Client,
                s3Presigner,
                new S3ObjectStorageProperties(
                        "promsearch-test-bucket",
                        "ap-northeast-2",
                        Duration.ofMinutes(10)
                )
        ));
    }

    @AfterEach
    void tearDown() {
        s3Presigner.close();
    }

    @Test
    void presignPutSignsExactLengthAndPreventsOverwrite() {
        PresignedUpload result = adapter.presignPut(
                "profiles/12/123e4567-e89b-12d3-a456-426614174000.jpg",
                "image/jpeg",
                1_024L
        );

        String query = URLDecoder.decode(result.uploadUrl().getRawQuery(), StandardCharsets.UTF_8);
        assertThat(query)
                .contains("X-Amz-SignedHeaders=content-length;content-type;host;if-none-match");
    }
}
