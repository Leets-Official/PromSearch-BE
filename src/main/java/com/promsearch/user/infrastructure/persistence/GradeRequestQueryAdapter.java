package com.promsearch.user.infrastructure.persistence;

import com.promsearch.prompt.domain.enums.PromptContentType;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class GradeRequestQueryAdapter implements LoadGradeRequestListPort, LoadGradeRequestSummaryPort {

    /*
     * 게시글 수/누적 추천 집계는 상대 프로필(USER-006, UserProfilePersistenceAdapter)과
     * 같은 공개 노출 기준을 따라야 하므로 콘텐츠 타입 필터를 동일하게 맞춘다.
     */
    private static final Set<PromptContentType> PUBLIC_PROFILE_CONTENT_TYPES = EnumSet.of(
            PromptContentType.FREE,
            PromptContentType.PREMIUM
    );

    private static final String SELECT_COLUMNS = """
            select gr.id, gr.userId,
                   (select u.email from UserJpaEntity u where u.id = gr.userId),
                   (select u.nickname from UserJpaEntity u where u.id = gr.userId),
                   gr.currentGrade, gr.requestedGrade, gr.status,
                   gr.createdAt, gr.processedAt,
                   (select count(p.id) from PostJpaEntity p
                    where p.userId = gr.userId and p.status = :activeStatus and p.deletedAt is null
                      and p.contentType in :contentTypes),
                   (select coalesce(sum(s.likeCount), 0) from PostJpaEntity p
                    left join p.statistics s
                    where p.userId = gr.userId and p.status = :activeStatus and p.deletedAt is null
                      and p.contentType in :contentTypes)
            from GradeRequestJpaEntity gr
            """;

    private final EntityManager entityManager;

    @Override
    public GradeRequestListInfo list(GradeRequestListQuery query) {
        String whereClause = query.status() != null ? " where gr.status = :status" : "";

        TypedQuery<Object[]> contentQuery = entityManager.createQuery(
                        SELECT_COLUMNS + whereClause + " order by gr.createdAt asc", Object[].class)
                .setParameter("activeStatus", PromptStatus.ACTIVE)
                .setParameter("contentTypes", PUBLIC_PROFILE_CONTENT_TYPES);
        if (query.status() != null) {
            contentQuery.setParameter("status", query.status());
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
        long totalElements = countQuery.getSingleResult();

        boolean hasNext = ((long) query.page() + 1) * query.size() < totalElements;
        return new GradeRequestListInfo(content, query.page(), query.size(), totalElements, hasNext);
    }

    @Override
    public GradeRequestSummaryInfo getById(Long gradeRequestId) {
        try {
            Object[] row = entityManager.createQuery(SELECT_COLUMNS + " where gr.id = :gradeRequestId", Object[].class)
                    .setParameter("activeStatus", PromptStatus.ACTIVE)
                    .setParameter("contentTypes", PUBLIC_PROFILE_CONTENT_TYPES)
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
                (String) row[3],
                (UserGrade) row[4],
                (UserGrade) row[5],
                (GradeRequestStatus) row[6],
                ((Number) row[9]).longValue(),
                ((Number) row[10]).longValue(),
                (Instant) row[7],
                (Instant) row[8]
        );
    }
}
