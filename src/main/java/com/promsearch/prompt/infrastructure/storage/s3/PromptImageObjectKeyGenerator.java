package com.promsearch.prompt.infrastructure.storage.s3;

import com.promsearch.prompt.application.port.out.storage.GeneratePromptImageObjectKeyPort;
import com.promsearch.prompt.domain.enums.PromptImageContentType;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 업로더별 원본 이미지 Object Key 생성 */
@Component
@RequiredArgsConstructor
public class PromptImageObjectKeyGenerator implements GeneratePromptImageObjectKeyPort {

    private final S3StorageProperties properties;

    /** 서버 UUID·허용 확장자 기반 원본 Object Key 반환 */
    @Override
    public String generateOriginal(Long uploaderId, UUID imageId, PromptImageContentType contentType) {
        String prefix = normalizePrefix(properties.originalPrefix());
        return "%s/%d/%s.%s".formatted(
                prefix,
                uploaderId,
                imageId,
                contentType.getExtension()
        );
    }

    private String normalizePrefix(String prefix) {
        return prefix.trim()
                .replaceAll("^/+", "")
                .replaceAll("/+$", "");
    }
}
