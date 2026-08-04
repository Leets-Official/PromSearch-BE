package com.promsearch.moderation.application.usecase;

import com.promsearch.moderation.application.usecase.dto.CreatePostReportCommand;

public interface CreatePostReportUseCase {

    void create(CreatePostReportCommand command);
}
