package com.promsearch.prompt.application.usecase;

/** 발행 가능한 워터마크 Outbox 작업을 외부 메시지 대기열로 전달 */
public interface PublishPendingPromptImageWatermarkJobsUseCase {

    /** 현재 발행 가능한 Outbox 한 배치를 처리하고 결과 건수 반환 */
    PublicationResult publishAvailable();

    /** 한 번의 발행 시도에서 선점·성공·실패한 작업 수 */
    record PublicationResult(int claimedCount, int publishedCount, int failedCount) {
    }
}
