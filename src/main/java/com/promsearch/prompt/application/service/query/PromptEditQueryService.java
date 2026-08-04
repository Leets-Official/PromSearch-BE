package com.promsearch.prompt.application.service.query;

import com.promsearch.prompt.application.port.out.prompt.LoadPromptEditPort;
import com.promsearch.prompt.application.port.out.prompt.LoadPromptEditPort.PromptEditProjection;
import com.promsearch.prompt.application.port.out.storage.PresignPromptImageDownloadPort;
import com.promsearch.prompt.application.usecase.GetPromptEditUseCase;
import com.promsearch.prompt.application.usecase.dto.PromptEditInfo;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromptEditQueryService implements GetPromptEditUseCase {

    private final LoadPromptEditPort loadPromptEditPort;
    private final PresignPromptImageDownloadPort imageStorage;

    @Override
    public PromptEditInfo get(Long promptId, Long userId) {
        if (promptId == null || promptId <= 0) {
            throw new PromptDomainException(PromptErrorCode.INVALID_ID);
        }
        if (userId == null || userId <= 0) {
            throw new PromptDomainException(PromptErrorCode.INVALID_PROMPT_USER_ID);
        }

        PromptEditProjection prompt = loadPromptEditPort.findById(promptId)
                .orElseThrow(() -> new PromptDomainException(PromptErrorCode.PROMPT_NOT_FOUND));
        if (!userId.equals(prompt.authorId())) {
            throw new PromptDomainException(PromptErrorCode.PROMPT_NOT_OWNED);
        }

        return new PromptEditInfo(
                prompt.promptId(), prompt.title(), prompt.description(), prompt.outputType(),
                prompt.jobTagIds(), prompt.taskTagIds(), prompt.aiModelTagIds(), prompt.customAiModel(),
                prompt.contentType(), prompt.promptBody(), prompt.visibility(),
                prompt.images().stream()
                        .map(image -> new PromptEditInfo.ImageInfo(
                                image.imageId(), imageStorage.presignGet(image.watermarkedObjectKey()),
                                image.sortOrder(), image.thumbnail()))
                        .toList(),
                prompt.status(), prompt.pricePoint(), prompt.updatedAt());
    }
}
