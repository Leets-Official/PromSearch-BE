package com.promsearch.prompt.infrastructure.storage.s3;

import com.promsearch.prompt.application.port.out.storage.GeneratePromptImageObjectKeyPort;
import com.promsearch.prompt.domain.enums.PromptImageContentType;
import com.promsearch.common.infrastructure.storage.StorageObjectKeyFactory;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 업로더별 원본 이미지 Object Key 생성 */
@Component
@RequiredArgsConstructor
public class PromptImageObjectKeyGenerator implements GeneratePromptImageObjectKeyPort {

    private final S3StorageProperties properties;
    private final StorageObjectKeyFactory objectKeyFactory;

    /** 서버 UUID·허용 확장자 기반 원본 Object Key 반환 */
    @Override
    public String generateOriginal(Long uploaderId, UUID imageId, PromptImageContentType contentType) {
        return objectKeyFactory.generate(
                properties.originalPrefix(),
                uploaderId,
                imageId,
                contentType.getExtension()
        );
    }

    /** 서버 UUID·허용 확장자 기반 워터마크 결과 Object Key 반환 */
    @Override
    public String generateWatermarked(Long uploaderId, UUID imageId, PromptImageContentType contentType) {
        return objectKeyFactory.generate(
                properties.watermarkedPrefix(),
                uploaderId,
                imageId,
                contentType.getExtension()
        );
    }

}
