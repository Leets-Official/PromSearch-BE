package com.promsearch.prompt.application.port.out.storage;

import com.promsearch.prompt.domain.enums.PromptImageContentType;
import java.util.UUID;

/** 이미지 Object Key 생성 포트 */
public interface GeneratePromptImageObjectKeyPort {

    /** 업로더·이미지 식별자 기반 원본 Object Key 반환 */
    String generateOriginal(Long uploaderId, UUID imageId, PromptImageContentType contentType);
}
