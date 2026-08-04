package com.promsearch.prompt.infrastructure.storage.s3;

import com.promsearch.prompt.application.port.out.storage.GeneratePromptImageObjectKeyPort;
import com.promsearch.prompt.domain.enums.PromptImageContentType;
import com.promsearch.global.infrastructure.storage.s3.S3StorageProperties;
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
        return generate(
                properties.originalPrefix(),
                uploaderId,
                imageId,
                contentType.getExtension()
        );
    }

    /** 서버 UUID·허용 확장자 기반 워터마크 결과 Object Key 반환 */
    @Override
    public String generateWatermarked(Long uploaderId, UUID imageId, PromptImageContentType contentType) {
        return generate(
                properties.watermarkedPrefix(),
                uploaderId,
                imageId,
                contentType.getExtension()
        );
    }

    /** 접두사·업로더·서버 UUID·허용 확장자를 조합해 충돌 없는 Object Key 생성 */
    private String generate(String prefix, Long uploaderId, UUID imageId, String extension) {
        return "%s/%d/%s.%s".formatted(
                normalizePrefix(prefix),
                uploaderId,
                imageId,
                extension
        );
    }

    /** 설정 접두사의 앞뒤 슬래시를 제거해 Object Key 중복 구분자를 방지 */
    private String normalizePrefix(String prefix) {
        return prefix.trim()
                .replaceAll("^/+", "")
                .replaceAll("/+$", "");
    }
}
