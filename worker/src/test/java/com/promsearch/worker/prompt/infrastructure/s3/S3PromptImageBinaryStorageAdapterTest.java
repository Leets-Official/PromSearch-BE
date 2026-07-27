package com.promsearch.worker.prompt.infrastructure.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.promsearch.prompt.application.port.out.storage.LoadPromptImageBinaryPort.StoredImage;
import com.promsearch.prompt.infrastructure.storage.s3.S3StorageProperties;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

class S3PromptImageBinaryStorageAdapterTest {

    private S3Client s3Client;
    private S3PromptImageBinaryStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        adapter = new S3PromptImageBinaryStorageAdapter(
                s3Client,
                new S3StorageProperties(
                        "promsearch-test",
                        "ap-northeast-2",
                        "prompt-images/original",
                        "prompt-images/watermarked",
                        Duration.ofMinutes(10)
                )
        );
    }

    @DisplayName("S3 원본 스트림의 바이너리와 Content-Type을 반환한다")
    @Test
    void loadOriginalImage() {
        byte[] bytes = {1, 2, 3};
        GetObjectResponse response = GetObjectResponse.builder()
                .contentLength((long) bytes.length)
                .contentType("image/png")
                .build();
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(
                new ResponseInputStream<>(
                        response,
                        AbortableInputStream.create(new ByteArrayInputStream(bytes))
                )
        );

        StoredImage storedImage =
                adapter.load("prompt-images/original/1/image.png");

        assertThat(storedImage.bytes()).containsExactly(bytes);
        assertThat(storedImage.contentType()).isEqualTo("image/png");
    }

    @DisplayName("워터마크 결과를 지정한 Key와 Content-Type으로 S3에 업로드한다")
    @Test
    void saveWatermarkedImage() {
        byte[] bytes = {4, 5, 6};

        adapter.save(
                "prompt-images/watermarked/1/image.png",
                "image/png",
                bytes
        );

        ArgumentCaptor<PutObjectRequest> requestCaptor =
                ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        assertThat(requestCaptor.getValue().bucket()).isEqualTo("promsearch-test");
        assertThat(requestCaptor.getValue().key())
                .isEqualTo("prompt-images/watermarked/1/image.png");
        assertThat(requestCaptor.getValue().contentType()).isEqualTo("image/png");
    }
}
