package com.promsearch.prompt.infrastructure.storage.s3;

import static org.assertj.core.api.Assertions.assertThat;

import com.promsearch.prompt.domain.enums.PromptImageContentType;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PromptImageObjectKeyGeneratorTest {

    @DisplayName("원본 이미지 Object Key는 설정 접두사와 업로더 UUID를 사용한다")
    @Test
    void generateOriginalObjectKey() {
        PromptImageObjectKeyGenerator generator = new PromptImageObjectKeyGenerator(
                new S3StorageProperties(
                        "test-bucket",
                        "ap-northeast-2",
                        "/prompt-images/original/",
                        "/prompt-images/watermarked/",
                        Duration.ofMinutes(10)
                )
        );
        UUID imageId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        String objectKey = generator.generateOriginal(10L, imageId, PromptImageContentType.JPEG);

        assertThat(objectKey)
                .isEqualTo("prompt-images/original/10/123e4567-e89b-12d3-a456-426614174000.jpg");
    }

    @DisplayName("워터마크 결과 Object Key는 원본과 다른 설정 접두사를 사용한다")
    @Test
    void generateWatermarkedObjectKey() {
        PromptImageObjectKeyGenerator generator = new PromptImageObjectKeyGenerator(
                new S3StorageProperties(
                        "test-bucket",
                        "ap-northeast-2",
                        "prompt-images/original",
                        "/prompt-images/watermarked/",
                        Duration.ofMinutes(10)
                )
        );
        UUID imageId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        String objectKey = generator.generateWatermarked(10L, imageId, PromptImageContentType.PNG);

        assertThat(objectKey)
                .isEqualTo("prompt-images/watermarked/10/123e4567-e89b-12d3-a456-426614174000.png");
    }
}
