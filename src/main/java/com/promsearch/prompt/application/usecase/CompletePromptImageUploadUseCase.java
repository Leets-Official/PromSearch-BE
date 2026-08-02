package com.promsearch.prompt.application.usecase;

import com.promsearch.prompt.application.usecase.dto.CompletePromptImageUploadCommand;
import com.promsearch.prompt.application.usecase.dto.PromptImageUploadInfo;

/** S3 이미지 업로드 완료 검증 유스케이스 */
public interface CompletePromptImageUploadUseCase {

    /** 예상 메타데이터·S3 객체 비교 및 UPLOADED 상태 전환 */
    PromptImageUploadInfo complete(CompletePromptImageUploadCommand command);
}
