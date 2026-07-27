package com.promsearch.worker.prompt.config;

import com.promsearch.prompt.application.port.out.promptimage.LoadPromptImagePort;
import com.promsearch.prompt.application.port.out.promptimage.SavePromptImagePort;
import com.promsearch.prompt.application.port.out.storage.LoadPromptImageBinaryPort;
import com.promsearch.prompt.application.port.out.storage.RenderPromptImageWatermarkPort;
import com.promsearch.prompt.application.port.out.storage.SavePromptImageBinaryPort;
import com.promsearch.prompt.application.service.command.PromptImageProcessingStateService;
import com.promsearch.prompt.application.service.command.PromptImageWatermarkProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** prompt 이미지 처리 UseCase와 Worker 기술 어댑터 조립 */
@Configuration(proxyBeanMethods = false)
public class WorkerPromptImageProcessingConfig {

    /** DB 상태 전이만 짧은 트랜잭션으로 처리하는 서비스 구성 */
    @Bean
    PromptImageProcessingStateService promptImageProcessingStateService(
            LoadPromptImagePort loadPromptImagePort,
            SavePromptImagePort savePromptImagePort
    ) {
        return new PromptImageProcessingStateService(
                loadPromptImagePort,
                savePromptImagePort
        );
    }

    /** S3·렌더러 어댑터를 prompt 처리 UseCase에 연결 */
    @Bean
    PromptImageWatermarkProcessor promptImageWatermarkProcessor(
            PromptImageProcessingStateService stateService,
            LoadPromptImageBinaryPort loadPromptImageBinaryPort,
            RenderPromptImageWatermarkPort renderPromptImageWatermarkPort,
            SavePromptImageBinaryPort savePromptImageBinaryPort
    ) {
        return new PromptImageWatermarkProcessor(
                stateService,
                loadPromptImageBinaryPort,
                renderPromptImageWatermarkPort,
                savePromptImageBinaryPort
        );
    }
}
