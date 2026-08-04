package com.promsearch.prompt.application.port.out.promptimage;

import com.promsearch.prompt.application.usecase.dto.PendingPromptImageWatermarkMessage;
import java.time.Instant;
import java.util.List;

/** 발행 가능한 Outbox 작업을 짧은 임대 시간 동안 선점 */
public interface ClaimPromptImageWatermarkJobsPort {

    /** 현재 발행 가능한 작업을 지정 개수만큼 조회하고 임대 만료 시각까지 선점 */
    List<PendingPromptImageWatermarkMessage> claimAvailable(
            Instant now,
            Instant claimedUntil,
            int batchSize
    );
}
