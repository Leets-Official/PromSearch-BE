package com.promsearch.prompt.application.usecase;

import com.promsearch.prompt.application.usecase.dto.IssuePromptImageUploadUrlsCommand;
import com.promsearch.prompt.application.usecase.dto.PromptImageUploadUrlsInfo;

/** 원본 이미지 직접 업로드 준비 유스케이스 */
public interface IssuePromptImageUploadUrlsUseCase {

    /** UPLOADING 이미지 자산 생성 및 Presigned PUT URL 반환 */
    PromptImageUploadUrlsInfo issue(IssuePromptImageUploadUrlsCommand command);
}
