package com.promsearch.moderation.application.service.query;

import com.promsearch.moderation.application.port.out.commentreport.CommentReportPageResult;
import com.promsearch.moderation.application.port.out.commentreport.LoadCommentReportPort;
import com.promsearch.moderation.application.port.out.postreport.LoadPostReportPort;
import com.promsearch.moderation.application.port.out.postreport.ReportPageResult;
import com.promsearch.moderation.application.port.out.target.LoadCommentReportTargetSummaryPort;
import com.promsearch.moderation.application.port.out.target.LoadPostReportTargetSummaryPort;
import com.promsearch.moderation.application.port.out.target.ReportTargetSummary;
import com.promsearch.moderation.application.usecase.SearchReportsUseCase;
import com.promsearch.moderation.application.usecase.dto.ReportInfo;
import com.promsearch.moderation.application.usecase.dto.ReportInfo.TargetSummaryInfo;
import com.promsearch.moderation.application.usecase.dto.ReportPageInfo;
import com.promsearch.moderation.application.usecase.dto.SearchReportsQuery;
import com.promsearch.moderation.domain.CommentReport;
import com.promsearch.moderation.domain.PostReport;
import com.promsearch.moderation.domain.enums.ReportTargetType;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostReportQueryService implements SearchReportsUseCase {

    private final LoadPostReportPort loadPostReportPort;
    private final LoadCommentReportPort loadCommentReportPort;
    private final LoadPostReportTargetSummaryPort loadPostReportTargetSummaryPort;
    private final LoadCommentReportTargetSummaryPort loadCommentReportTargetSummaryPort;

    @Override
    public ReportPageInfo searchReports(SearchReportsQuery query) {
        if (query.targetType() == null) {
            return searchAllReports(query);
        }
        return switch (query.targetType()) {
            case POST -> searchPostReports(query);
            case COMMENT -> searchCommentReports(query);
        };
    }

    private ReportPageInfo searchPostReports(SearchReportsQuery query) {
        ReportPageResult result = loadPostReportPort.search(query.status(), query.page(), query.size());
        Map<Long, ReportTargetSummary> summaries = indexById(
                loadPostReportTargetSummaryPort.list(
                        result.content().stream().map(PostReport::getPostId).distinct().toList()
                )
        );
        List<ReportInfo> content = result.content().stream()
                .map(report -> ReportInfo.from(report, summaryOrDeleted(summaries, report.getPostId())))
                .toList();

        return new ReportPageInfo(content, result.totalElements());
    }

    private ReportPageInfo searchCommentReports(SearchReportsQuery query) {
        CommentReportPageResult result = loadCommentReportPort.search(query.status(), query.page(), query.size());
        Map<Long, ReportTargetSummary> summaries = indexById(
                loadCommentReportTargetSummaryPort.list(
                        result.content().stream().map(CommentReport::getCommentId).distinct().toList()
                )
        );
        List<ReportInfo> content = result.content().stream()
                .map(report -> ReportInfo.from(report, summaryOrDeleted(summaries, report.getCommentId())))
                .toList();

        return new ReportPageInfo(content, result.totalElements());
    }

    private ReportPageInfo searchAllReports(SearchReportsQuery query) {
        /*
         * post_reports/comment_reports는 별도 테이블이라 createdAt 기준 병합 정렬이 필요하다.
         * 두 목록 모두 createdAt desc로 정렬되어 있으므로, 각 목록에서 상위 (page+1)*size개만 가져와
         * 병합해도 요청한 페이지 구간을 정확히 재구성할 수 있다(정렬된 두 목록의 상위 K개 합집합은
         * 병합 결과의 상위 K개를 항상 포함한다).
         */
        int limit = (query.page() + 1) * query.size();
        ReportPageInfo postPage = searchPostReports(new SearchReportsQuery(ReportTargetType.POST, query.status(), 0, limit));
        ReportPageInfo commentPage = searchCommentReports(
                new SearchReportsQuery(ReportTargetType.COMMENT, query.status(), 0, limit));

        List<ReportInfo> merged = Stream.concat(postPage.content().stream(), commentPage.content().stream())
                .sorted(Comparator.comparing(ReportInfo::createdAt).reversed())
                .toList();

        int fromIndex = Math.min(query.page() * query.size(), merged.size());
        int toIndex = Math.min(fromIndex + query.size(), merged.size());
        long totalElements = postPage.totalElements() + commentPage.totalElements();

        return new ReportPageInfo(merged.subList(fromIndex, toIndex), totalElements);
    }

    private Map<Long, ReportTargetSummary> indexById(List<ReportTargetSummary> summaries) {
        return summaries.stream()
                .collect(Collectors.toMap(ReportTargetSummary::targetId, Function.identity()));
    }

    private TargetSummaryInfo summaryOrDeleted(Map<Long, ReportTargetSummary> summaries, Long targetId) {
        ReportTargetSummary summary = summaries.get(targetId);
        return summary == null ? TargetSummaryInfo.notFound() : TargetSummaryInfo.from(summary);
    }
}
