package com.promsearch.prompt.application.port.out.promptimage;

import com.promsearch.prompt.application.usecase.dto.PendingPromptImageWatermarkMessage;

/** 워터마크 작업 메시지를 외부 대기열에 발행 */
public interface PublishPromptImageWatermarkJobPort {

    /** 직렬화된 워터마크 작업을 외부 메시지 대기열에 발행 */
    void publish(PendingPromptImageWatermarkMessage message);
}
