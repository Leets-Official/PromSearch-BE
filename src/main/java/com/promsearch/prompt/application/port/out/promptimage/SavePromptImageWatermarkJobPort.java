package com.promsearch.prompt.application.port.out.promptimage;

import com.promsearch.prompt.application.usecase.dto.PromptImageWatermarkJob;

/** 워터마크 작업을 외부 메시지 브로커로 전달하기 전에 영속 저장 */
public interface SavePromptImageWatermarkJobPort {

    /** 이미지 상태 변경과 같은 트랜잭션에서 발행 대기 작업 저장 */
    void save(PromptImageWatermarkJob job);
}
