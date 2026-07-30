package com.promsearch.prompt.application.port.out.promptimage;

import com.promsearch.prompt.domain.PromptImage;
import java.util.UUID;

/** 이미지 자산 조회 포트 */
public interface LoadPromptImagePort {

    /** 이미지 식별자 기반 필수 조회 */
    PromptImage getById(UUID imageId);
}
