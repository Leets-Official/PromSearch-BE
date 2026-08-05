package com.promsearch.moderation.application.usecase;

import com.promsearch.moderation.application.usecase.dto.CreateCommentReportCommand;

public interface CreateCommentReportUseCase {

    void create(CreateCommentReportCommand command);
}
