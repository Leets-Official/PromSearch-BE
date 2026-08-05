package com.promsearch.user.infrastructure.persistence;

import com.promsearch.user.application.port.out.user.LoadOriginUserListPort;
import com.promsearch.user.application.usecase.dto.OriginUserListInfo;
import com.promsearch.user.application.usecase.dto.OriginUserListQuery;
import com.promsearch.user.application.usecase.dto.OriginUserSummaryInfo;
import com.promsearch.user.domain.enums.UserGrade;
import com.promsearch.user.domain.enums.UserStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OriginUserQueryAdapter implements LoadOriginUserListPort {

    private static final String SELECT_COLUMNS = """
            select u.id, u.nickname
            from UserJpaEntity u
            where u.grade = :grade and u.status = :status
            """;

    private final EntityManager entityManager;

    @Override
    public OriginUserListInfo list(OriginUserListQuery query) {
        TypedQuery<Object[]> contentQuery = entityManager.createQuery(
                SELECT_COLUMNS + " order by u.updatedAt desc", Object[].class);
        List<OriginUserSummaryInfo> content = applyGradeAndStatus(contentQuery)
                .setFirstResult(toOffset(query.page(), query.size()))
                .setMaxResults(query.size())
                .getResultList()
                .stream()
                .map(row -> new OriginUserSummaryInfo((Long) row[0], (String) row[1]))
                .toList();

        TypedQuery<Long> countQuery = entityManager.createQuery(
                "select count(u.id) from UserJpaEntity u where u.grade = :grade and u.status = :status", Long.class);
        long totalElements = applyGradeAndStatus(countQuery).getSingleResult();

        boolean hasNext = ((long) query.page() + 1) * query.size() < totalElements;
        return new OriginUserListInfo(content, query.page(), query.size(), totalElements, hasNext);
    }

    private <T> TypedQuery<T> applyGradeAndStatus(TypedQuery<T> query) {
        return query
                .setParameter("grade", UserGrade.ORIGIN)
                .setParameter("status", UserStatus.ACTIVE);
    }

    private int toOffset(int page, int size) {
        return Math.toIntExact((long) page * size);
    }
}
