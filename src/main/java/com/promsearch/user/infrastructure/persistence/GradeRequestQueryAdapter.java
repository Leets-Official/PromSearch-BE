package com.promsearch.user.infrastructure.persistence;

import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.user.application.port.out.graderequest.LoadGradeRequestListPort;
import com.promsearch.user.application.port.out.graderequest.LoadGradeRequestSummaryPort;
import com.promsearch.user.application.usecase.dto.GradeRequestListInfo;
import com.promsearch.user.application.usecase.dto.GradeRequestListQuery;
import com.promsearch.user.application.usecase.dto.GradeRequestSummaryInfo;
import com.promsearch.user.domain.enums.GradeRequestStatus;
import com.promsearch.user.domain.enums.UserGrade;
import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class GradeRequestQueryAdapter implements LoadGradeRequestListPort, LoadGradeRequestSummaryPort {

    private static final String SELECT_COLUMNS = """
            select gr.id, gr.userId,
                   (select u.nickname from UserJpaEntity u where u.id = gr.userId),
                   gr.currentGrade, gr.requestedGrade, gr.status,
                   gr.createdAt, gr.processedAt,
                   (select count(p.id) from PostJpaEntity p
                    where p.userId = gr.userId and p.status = :activeStatus and p.deletedAt is null),
                   (select coalesce(sum(s.likeCount), 0) from PostJpaEntity p
                    left join p.statistics s
                    where p.userId = gr.userId and p.status = :activeStatus and p.deletedAt is null)
            from GradeRequestJpaEntity gr
            """;

    private final EntityManager entityManager;

    private static final String Q_CONDITION = """
            exists (
                select 1 from UserJpaEntity u
                where u.id = gr.userId
                  and (lower(u.email) like :q or lower(u.nickname) like :q)
            )
            """;

    @Override
    public GradeRequestListInfo list(GradeRequestListQuery query) {
        String whereClause = buildWhereClause(query);
        String likeQ = toLikePattern(query.q());

        TypedQuery<Object[]> contentQuery = entityManager.createQuery(
                        SELECT_COLUMNS + whereClause + " order by gr.createdAt asc", Object[].class)
                .setParameter("activeStatus", PromptStatus.ACTIVE);
        if (query.status() != null) {
            contentQuery.setParameter("status", query.status());
        }
        if (likeQ != null) {
            contentQuery.setParameter("q", likeQ);
        }
        List<GradeRequestSummaryInfo> content = contentQuery
                .setFirstResult(toOffset(query.page(), query.size()))
                .setMaxResults(query.size())
                .getResultList()
                .stream()
                .map(this::toSummaryInfo)
                .toList();

        TypedQuery<Long> countQuery = entityManager.createQuery(
                "select count(gr) from GradeRequestJpaEntity gr" + whereClause, Long.class);
        if (query.status() != null) {
            countQuery.setParameter("status", query.status());
        }
        if (likeQ != null) {
            countQuery.setParameter("q", likeQ);
        }
        long totalElements = countQuery.getSingleResult();

        boolean hasNext = ((long) query.page() + 1) * query.size() < totalElements;
        return new GradeRequestListInfo(content, query.page(), query.size(), totalElements, hasNext);
    }

    private String buildWhereClause(GradeRequestListQuery query) {
        List<String> conditions = new ArrayList<>();
        if (query.status() != null) {
            conditions.add("gr.status = :status");
        }
        if (query.q() != null) {
            conditions.add(Q_CONDITION);
        }
        return conditions.isEmpty() ? "" : " where " + String.join(" and ", conditions);
    }

    private String toLikePattern(String q) {
        return q == null ? null : "%" + q.toLowerCase() + "%";
    }

    @Override
    public GradeRequestSummaryInfo getById(Long gradeRequestId) {
        try {
            Object[] row = entityManager.createQuery(SELECT_COLUMNS + " where gr.id = :gradeRequestId", Object[].class)
                    .setParameter("activeStatus", PromptStatus.ACTIVE)
                    .setParameter("gradeRequestId", gradeRequestId)
                    .getSingleResult();
            return toSummaryInfo(row);
        } catch (NoResultException e) {
            throw new UserDomainException(UserErrorCode.GRADE_REQUEST_NOT_FOUND);
        }
    }

    private int toOffset(int page, int size) {
        return Math.toIntExact((long) page * size);
    }

    private GradeRequestSummaryInfo toSummaryInfo(Object[] row) {
        return new GradeRequestSummaryInfo(
                (Long) row[0],
                (Long) row[1],
                (String) row[2],
                (String) row[2],
                (UserGrade) row[3],
                (UserGrade) row[4],
                (GradeRequestStatus) row[5],
                ((Number) row[8]).longValue(),
                ((Number) row[9]).longValue(),
                (Instant) row[6],
                (Instant) row[7]
        );
    }
}
