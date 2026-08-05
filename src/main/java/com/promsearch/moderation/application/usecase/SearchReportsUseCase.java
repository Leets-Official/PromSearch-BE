package com.promsearch.moderation.application.usecase;

import com.promsearch.moderation.application.usecase.dto.ReportPageInfo;
import com.promsearch.moderation.application.usecase.dto.SearchReportsQuery;

public interface SearchReportsUseCase {

    ReportPageInfo searchReports(SearchReportsQuery query);
}
