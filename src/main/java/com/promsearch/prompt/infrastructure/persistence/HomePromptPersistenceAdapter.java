package com.promsearch.prompt.infrastructure.persistence;

import com.promsearch.community.domain.enums.InteractionType;
import com.promsearch.prompt.application.HomePromptAuthorInfo;
import com.promsearch.prompt.application.HomePromptListInfo;
import com.promsearch.prompt.application.HomePromptListQuery;
import com.promsearch.prompt.application.HomePromptStatisticsInfo;
import com.promsearch.prompt.application.HomePromptSummaryInfo;
import com.promsearch.prompt.application.HomePromptTagInfo;
import com.promsearch.prompt.application.HomePromptViewerInteractionInfo;
import com.promsearch.prompt.application.port.out.HomePromptReader;
import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.domain.enums.TagType;
import com.promsearch.user.domain.enums.UserStatus;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class HomePromptPersistenceAdapter implements HomePromptReader {

    /*
     * The week-5 product notes removed MASTER from the exposed content policy.
     * Until the enum is cleaned up in a separate PR, home lists intentionally expose
     * only FREE and PREMIUM prompts.
     */
    private static final Set<PromptContentType> HOME_CONTENT_TYPES = EnumSet.of(
            PromptContentType.FREE,
            PromptContentType.PREMIUM
    );

    private final EntityManager entityManager;

    @Override
    public HomePromptListInfo listPopularPrompts(HomePromptListQuery query) {
        // Popular cards are ranked by likes, with newest/id ordering as deterministic tie-breakers.
        String whereClause = """
                p.status = :activeStatus
                and p.deletedAt is null
                and p.contentType in :contentTypes
                and u.id = p.userId
                and u.status = :activeUserStatus
                """;

        List<Object[]> rows = createBasePromptQuery(whereClause + """
                order by coalesce(s.likeCount, 0) desc, p.createdAt desc, p.id desc
                """)
                .setParameter("activeStatus", PromptStatus.ACTIVE)
                .setParameter("contentTypes", HOME_CONTENT_TYPES)
                .setParameter("activeUserStatus", UserStatus.ACTIVE)
                .setFirstResult(toOffset(query.page(), query.size()))
                .setMaxResults(query.size())
                .getResultList();

        long totalElements = countBasePrompts(whereClause)
                .setParameter("activeStatus", PromptStatus.ACTIVE)
                .setParameter("contentTypes", HOME_CONTENT_TYPES)
                .setParameter("activeUserStatus", UserStatus.ACTIVE)
                .getSingleResult();

        return toListInfo(rows, query, totalElements);
    }

    @Override
    public HomePromptListInfo listJobPrompts(HomePromptListQuery query) {
        // Job sections use the official JOB tag taxonomy from prompt tags, not free-form text matching.
        String whereClause = """
                p.status = :activeStatus
                and p.deletedAt is null
                and p.contentType in :contentTypes
                and u.id = p.userId
                and u.status = :activeUserStatus
                and t.id = :jobTagId
                and t.tagType = :jobTagType
                """;

        List<Object[]> rows = entityManager.createQuery("""
                        select p.id, p.title, p.thumbnailImageUrl, p.outputType, p.contentType, p.pricePoint,
                               p.createdAt, u.id, u.nickname, u.profileImageUrl,
                               coalesce(s.viewCount, 0), coalesce(s.likeCount, 0),
                               coalesce(s.commentCount, 0), coalesce(s.copyCount, 0)
                        from PostJpaEntity p
                        join p.postTags pt
                        join pt.tag t
                        left join p.statistics s,
                             UserJpaEntity u
                        where """ + whereClause + """
                        order by p.createdAt desc, p.id desc
                        """, Object[].class)
                .setParameter("activeStatus", PromptStatus.ACTIVE)
                .setParameter("contentTypes", HOME_CONTENT_TYPES)
                .setParameter("activeUserStatus", UserStatus.ACTIVE)
                .setParameter("jobTagId", query.jobTagId())
                .setParameter("jobTagType", TagType.JOB)
                .setFirstResult(toOffset(query.page(), query.size()))
                .setMaxResults(query.size())
                .getResultList();

        long totalElements = entityManager.createQuery("""
                        select count(p.id)
                        from PostJpaEntity p
                        join p.postTags pt
                        join pt.tag t,
                             UserJpaEntity u
                        where """ + whereClause, Long.class)
                .setParameter("activeStatus", PromptStatus.ACTIVE)
                .setParameter("contentTypes", HOME_CONTENT_TYPES)
                .setParameter("activeUserStatus", UserStatus.ACTIVE)
                .setParameter("jobTagId", query.jobTagId())
                .setParameter("jobTagType", TagType.JOB)
                .getSingleResult();

        return toListInfo(rows, query, totalElements);
    }

    private jakarta.persistence.TypedQuery<Object[]> createBasePromptQuery(String whereAndOrderClause) {
        return entityManager.createQuery("""
                        select p.id, p.title, p.thumbnailImageUrl, p.outputType, p.contentType, p.pricePoint,
                               p.createdAt, u.id, u.nickname, u.profileImageUrl,
                               coalesce(s.viewCount, 0), coalesce(s.likeCount, 0),
                               coalesce(s.commentCount, 0), coalesce(s.copyCount, 0)
                        from PostJpaEntity p
                        left join p.statistics s,
                             UserJpaEntity u
                        where """ + whereAndOrderClause, Object[].class);
    }

    private jakarta.persistence.TypedQuery<Long> countBasePrompts(String whereClause) {
        return entityManager.createQuery("""
                        select count(p.id)
                        from PostJpaEntity p,
                             UserJpaEntity u
                        where """ + whereClause, Long.class);
    }

    private HomePromptListInfo toListInfo(List<Object[]> rows, HomePromptListQuery query, long totalElements) {
        List<Long> promptIds = rows.stream()
                .map(row -> number(row[0]))
                .toList();

        // Load card-side data in batches so a 12-card home page does not issue per-card queries.
        Map<Long, String> thumbnailImages = findThumbnailImages(promptIds);
        Map<Long, List<HomePromptTagInfo>> tags = findTags(promptIds);
        Map<Long, HomePromptViewerInteractionInfo> interactions = findViewerInteractions(query.viewerUserId(), promptIds);

        List<HomePromptSummaryInfo> prompts = rows.stream()
                .map(row -> toSummaryInfo(row, thumbnailImages, tags, interactions))
                .toList();

        boolean hasNext = ((long) query.page() + 1) * query.size() < totalElements;
        return new HomePromptListInfo(prompts, query.page(), query.size(), totalElements, hasNext);
    }

    private HomePromptSummaryInfo toSummaryInfo(
            Object[] row,
            Map<Long, String> thumbnailImages,
            Map<Long, List<HomePromptTagInfo>> tags,
            Map<Long, HomePromptViewerInteractionInfo> interactions
    ) {
        Long promptId = number(row[0]);
        String thumbnailImageUrl = (String) row[2];
        if (thumbnailImageUrl == null || thumbnailImageUrl.isBlank()) {
            thumbnailImageUrl = thumbnailImages.get(promptId);
        }

        return new HomePromptSummaryInfo(
                promptId,
                (String) row[1],
                thumbnailImageUrl,
                (PromptOutputType) row[3],
                (PromptContentType) row[4],
                number(row[5]),
                new HomePromptAuthorInfo(number(row[7]), (String) row[8], (String) row[9]),
                new HomePromptStatisticsInfo(
                        count(row[10]),
                        count(row[11]),
                        count(row[12]),
                        count(row[13])
                ),
                interactions.getOrDefault(promptId, HomePromptViewerInteractionInfo.none()),
                tags.getOrDefault(promptId, List.of()),
                (Instant) row[6]
        );
    }

    private Map<Long, String> findThumbnailImages(List<Long> promptIds) {
        if (promptIds.isEmpty()) {
            return Map.of();
        }

        List<Object[]> rows = entityManager.createQuery("""
                        select image.post.id, image.imageUrl
                        from PostImageJpaEntity image
                        where image.post.id in :promptIds
                          and image.thumbnail = true
                        order by image.post.id asc, image.sortOrder asc, image.id asc
                        """, Object[].class)
                .setParameter("promptIds", promptIds)
                .getResultList();

        Map<Long, String> thumbnails = new HashMap<>();
        for (Object[] row : rows) {
            thumbnails.putIfAbsent(number(row[0]), (String) row[1]);
        }
        return thumbnails;
    }

    private Map<Long, List<HomePromptTagInfo>> findTags(List<Long> promptIds) {
        if (promptIds.isEmpty()) {
            return Map.of();
        }

        List<Object[]> rows = entityManager.createQuery("""
                        select p.id, tag.id, tag.tagType, tag.tagName
                        from PostJpaEntity p
                        join p.postTags pt
                        join pt.tag tag
                        where p.id in :promptIds
                        order by p.id asc, tag.tagType asc, tag.tagName asc
                        """, Object[].class)
                .setParameter("promptIds", promptIds)
                .getResultList();

        Map<Long, List<HomePromptTagInfo>> tags = new LinkedHashMap<>();
        for (Object[] row : rows) {
            tags.computeIfAbsent(number(row[0]), ignored -> new ArrayList<>())
                    .add(new HomePromptTagInfo(number(row[1]), (TagType) row[2], (String) row[3]));
        }
        return tags;
    }

    private Map<Long, HomePromptViewerInteractionInfo> findViewerInteractions(Long viewerUserId, List<Long> promptIds) {
        if (viewerUserId == null || promptIds.isEmpty()) {
            return Map.of();
        }

        List<Object[]> rows = entityManager.createQuery("""
                        select interaction.postId, interaction.interactionType
                        from PostInteractionJpaEntity interaction
                        where interaction.userId = :viewerUserId
                          and interaction.postId in :promptIds
                        """, Object[].class)
                .setParameter("viewerUserId", viewerUserId)
                .setParameter("promptIds", promptIds)
                .getResultList();

        Map<Long, EnumSet<InteractionType>> grouped = new HashMap<>();
        for (Object[] row : rows) {
            grouped.computeIfAbsent(number(row[0]), ignored -> EnumSet.noneOf(InteractionType.class))
                    .add((InteractionType) row[1]);
        }

        Map<Long, HomePromptViewerInteractionInfo> interactions = new HashMap<>();
        grouped.forEach((promptId, types) -> interactions.put(
                promptId,
                new HomePromptViewerInteractionInfo(
                        types.contains(InteractionType.LIKE),
                        types.contains(InteractionType.BOOKMARK)
                )
        ));
        return interactions;
    }

    private int toOffset(int page, int size) {
        return Math.toIntExact((long) page * size);
    }

    private Long number(Object value) {
        return ((Number) value).longValue();
    }

    private long count(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }
}
