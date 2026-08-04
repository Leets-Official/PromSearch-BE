package com.promsearch.prompt.application.usecase;

import com.promsearch.prompt.application.usecase.dto.PromptImageWatermarkJob;

/** 워터마크 작업 한 건 처리 진입점 */
public interface ProcessPromptImageWatermarkUseCase {

    /** 메시지의 원본을 처리하고 결과 저장과 READY 상태 전환까지 수행 */
    void process(PromptImageWatermarkJob job);
}
