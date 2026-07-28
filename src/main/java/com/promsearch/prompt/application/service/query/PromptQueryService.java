package com.promsearch.prompt.application.service.query;

import com.promsearch.prompt.application.port.out.prompt.LoadPromptPort;
import com.promsearch.prompt.application.port.out.prompt.PromptPageResult;
import com.promsearch.prompt.application.usecase.GetMyPromptInsightsUseCase;
import com.promsearch.prompt.application.usecase.ListMyPromptsUseCase;
import com.promsearch.prompt.application.usecase.dto.ListMyPromptsQuery;
import com.promsearch.prompt.application.usecase.dto.MyPromptPageInfo;
import com.promsearch.prompt.application.usecase.dto.MyPromptSummaryInfo;
import com.promsearch.prompt.application.usecase.dto.PromptInsightInfo;
import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromptQueryService implements ListMyPromptsUseCase, GetMyPromptInsightsUseCase {

    private final LoadPromptPort loadPromptPort;

    @Override
    public MyPromptPageInfo listMyPublishedPrompts(ListMyPromptsQuery query) {
        if (query.status() != PromptStatus.ACTIVE) {
            throw new PromptDomainException(PromptErrorCode.INVALID_PROMPT_STATUS);
        }

        PromptPageResult result = loadPromptPort.listByUserIdAndStatus(
                query.userId(),
                query.status(),
                query.page(),
                query.size()
        );

        return new MyPromptPageInfo(
                result.content().stream().map(MyPromptSummaryInfo::from).toList(),
                result.totalElements()
        );
    }

    @Override
    public PromptInsightInfo getMyPromptInsights(Long userId) {
        return PromptInsightInfo.from(loadPromptPort.sumInsightsByUserId(userId));
    }
}
