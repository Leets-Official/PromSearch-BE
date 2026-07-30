package com.promsearch.prompt.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promsearch.prompt.application.port.out.promptimage.SavePromptImageWatermarkJobPort;
import com.promsearch.prompt.application.usecase.dto.PromptImageWatermarkJob;
import com.promsearch.prompt.infrastructure.persistence.entity.PromptImageWatermarkOutboxJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 워터마크 작업 메시지를 JSON으로 직렬화하여 Outbox에 저장 */
@Component
@RequiredArgsConstructor
public class PromptImageWatermarkOutboxPersistenceAdapter
        implements SavePromptImageWatermarkJobPort {

    private final PromptImageWatermarkOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /** SQS 발행 전 원본 메시지와 발행 상태를 함께 저장 */
    @Override
    public void save(PromptImageWatermarkJob job) {
        outboxRepository.saveAndFlush(
                PromptImageWatermarkOutboxJpaEntity.pending(job, serialize(job))
        );
    }

    /** Worker가 그대로 역직렬화할 수 있도록 버전이 포함된 작업 메시지를 JSON으로 변환 */
    private String serialize(PromptImageWatermarkJob job) {
        try {
            return objectMapper.writeValueAsString(job);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("워터마크 작업 메시지를 직렬화할 수 없습니다.", exception);
        }
    }
}
