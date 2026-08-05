package com.promsearch.moderation.application.service.query;

import com.promsearch.moderation.application.port.out.commentreport.CommentReportPageResult;
import com.promsearch.moderation.application.port.out.commentreport.LoadCommentReportPort;
import com.promsearch.moderation.application.port.out.postreport.LoadPostReportPort;
import com.promsearch.moderation.application.port.out.postreport.ReportPageResult;
import com.promsearch.moderation.application.usecase.SearchReportsUseCase;
import com.promsearch.moderation.application.usecase.dto.ReportInfo;
import com.promsearch.moderation.application.usecase.dto.ReportPageInfo;
import com.promsearch.moderation.application.usecase.dto.SearchReportsQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostReportQueryService implements SearchReportsUseCase {

    private final LoadPostReportPort loadPostReportPort;
    private final LoadCommentReportPort loadCommentReportPort;

    @Override
    public ReportPageInfo searchReports(SearchReportsQuery query) {
        return switch (query.targetType()) {
            case POST -> searchPostReports(query);
            case COMMENT -> searchCommentReports(query);
        };
    }

    private ReportPageInfo searchPostReports(SearchReportsQuery query) {
        ReportPageResult result = loadPostReportPort.search(query.status(), query.q(), query.page(), query.size());

        return new ReportPageInfo(
                result.content().stream().map(ReportInfo::from).toList(),
                result.totalElements()
        );
    }

    private ReportPageInfo searchCommentReports(SearchReportsQuery query) {
        CommentReportPageResult result =
                loadCommentReportPort.search(query.status(), query.q(), query.page(), query.size());

        return new ReportPageInfo(
                result.content().stream().map(ReportInfo::from).toList(),
                result.totalElements()
        );
    }
}
