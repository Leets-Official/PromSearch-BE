package com.promsearch.prompt.application.usecase;

import com.promsearch.prompt.application.usecase.dto.GetPromptImageStatusesQuery;
import com.promsearch.prompt.application.usecase.dto.PromptImageStatusesInfo;

/** 인증 사용자의 프롬프트 이미지 처리 상태 일괄 조회 UseCase */
public interface GetPromptImageStatusesUseCase {

    /** 요청 순서대로 이미지 처리 상태를 조회한다. */
    PromptImageStatusesInfo getStatuses(GetPromptImageStatusesQuery query);
}
