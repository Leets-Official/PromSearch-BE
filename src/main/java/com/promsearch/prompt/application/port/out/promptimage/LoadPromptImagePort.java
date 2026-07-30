package com.promsearch.prompt.application.port.out.promptimage;

import com.promsearch.prompt.domain.PromptImage;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** 이미지 자산 조회 포트 */
public interface LoadPromptImagePort {

    /** 이미지 식별자 기반 필수 조회 */
    PromptImage getById(UUID imageId);

    /** 프롬프트 연결 경쟁을 막기 위해 요청 이미지 전체를 잠금 조회 */
    List<PromptImage> batchGetByIdsForUpdate(Collection<UUID> imageIds);

    /** 이미지 식별자 목록 기반 일괄 조회 */
    List<PromptImage> listByIds(Collection<UUID> imageIds);
}
