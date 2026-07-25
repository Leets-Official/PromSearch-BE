package com.promsearch.auth.application.usecase;

import com.promsearch.auth.application.usecase.dto.ReissueCommand;
import com.promsearch.auth.application.usecase.dto.ReissueInfo;

public interface ReissueUseCase {

    ReissueInfo reissue(ReissueCommand command);
}
