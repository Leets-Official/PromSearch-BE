package com.promsearch.prompt.infrastructure.query;

import com.promsearch.community.application.port.out.bookmark.LoadBookmarkListPort;
import com.promsearch.community.application.usecase.dto.BookmarkListInfo;
import com.promsearch.community.application.usecase.dto.BookmarkListInfo.AuthorInfo;
import com.promsearch.community.application.usecase.dto.BookmarkListInfo.BookmarkPromptInfo;
import com.promsearch.community.application.usecase.dto.BookmarkListInfo.TagInfo;
import com.promsearch.community.application.usecase.dto.BookmarkListQuery;
import com.promsearch.prompt.application.port.out.storage.PresignPromptImageDownloadPort;
import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import com.promsearch.prompt.domain.enums.TagType;
import com.promsearch.prompt.infrastructure.persistence.PromptImageRepository;
import com.promsearch.prompt.infrastructure.persistence.entity.PromptImageJpaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookmarkListQueryAdapter implements LoadBookmarkListPort {

    private static final String BASE_FROM = """
            from PostInteractionJpaEntity interaction
            join PostJpaEntity post on post.id = interaction.postId
            join UserJpaEntity author on author.id = post.userId
            left join PostStatisticsJpaEntity statistics on statistics.postId = post.id
            where interaction.userId = :userId
              and interaction.interactionType = com.promsearch.community.domain.enums.InteractionType.BOOKMARK
              and post.status = com.promsearch.prompt.domain.enums.PromptStatus.ACTIVE
              and post.visibility = com.promsearch.prompt.domain.enums.PromptVisibility.PUBLIC
              and post.deletedAt is null
              and author.status = com.promsearch.user.domain.enums.UserStatus.ACTIVE
              and author.deletedAt is null
            """;

    private final EntityManager entityManager;
    private final PromptImageRepository promptImageRepository;
    private final PresignPromptImageDownloadPort presignPromptImageDownloadPort;

    @Override
    public BookmarkListInfo load(BookmarkListQuery query) {
        String filters = filters(query);
        TypedQuery<Object[]> contentQuery = entityManager.createQuery("""
                select interaction.postId,
                       post.title,
                       post.contentType,
                       post.outputType,
                       post.pricePoint,
                       coalesce(statistics.viewCount, 0),
                       coalesce(statistics.likeCount, 0),
                       author.id,
                       author.nickname,
                       author.profileImageUrl,
                       interaction.createdAt
                """ + BASE_FROM + filters
                + " order by interaction.createdAt desc, interaction.id desc", Object[].class);
        applyParameters(contentQuery, query);
        List<Object[]> rows = contentQuery
                .setFirstResult(Math.multiplyExact(query.page(), query.size()))
                .setMaxResults(query.size())
                .getResultList();

        TypedQuery<Long> countQuery = entityManager.createQuery(
                "select count(interaction.id) " + BASE_FROM + filters,
                Long.class
        );
        applyParameters(countQuery, query);
        long totalElements = countQuery.getSingleResult();

        List<Long> promptIds = rows.stream()
                .map(row -> (Long) row[0])
                .toList();
        Map<Long, String> thumbnails = loadThumbnails(promptIds);
        Map<Long, List<TagInfo>> tags = loadTags(promptIds);

        List<BookmarkPromptInfo> content = rows.stream()
                .map(row -> toInfo(row, thumbnails, tags))
                .toList();
        boolean hasNext = ((long) query.page() + 1) * query.size() < totalElements;
        return new BookmarkListInfo(
                content,
                query.page(),
                query.size(),
                totalElements,
                hasNext
        );
    }

    private String filters(BookmarkListQuery query) {
        StringBuilder filters = new StringBuilder();
        if (query.taskTagId() != null) {
            filters.append("""
                      and exists (
                          select postTag.id
                          from PostTagJpaEntity postTag
                          where postTag.post.id = post.id
                            and postTag.tag.id = :taskTagId
                            and postTag.tag.tagType = com.promsearch.prompt.domain.enums.TagType.TASK
                      )
                    """);
        }
        if (query.aiModelTagId() != null) {
            filters.append("""
                      and exists (
                          select postTag.id
                          from PostTagJpaEntity postTag
                          where postTag.post.id = post.id
                            and postTag.tag.id = :aiModelTagId
                            and postTag.tag.tagType = com.promsearch.prompt.domain.enums.TagType.AI_MODEL
                      )
                    """);
        }
        if (query.outputType() != null) {
            filters.append(" and post.outputType = :outputType");
        }
        return filters.toString();
    }

    private void applyParameters(TypedQuery<?> typedQuery, BookmarkListQuery query) {
        typedQuery.setParameter("userId", query.userId());
        if (query.taskTagId() != null) {
            typedQuery.setParameter("taskTagId", query.taskTagId());
        }
        if (query.aiModelTagId() != null) {
            typedQuery.setParameter("aiModelTagId", query.aiModelTagId());
        }
        if (query.outputType() != null) {
            typedQuery.setParameter("outputType", query.outputType());
        }
    }

    private Map<Long, String> loadThumbnails(List<Long> promptIds) {
        if (promptIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> result = new HashMap<>();
        for (PromptImageJpaEntity image : promptImageRepository.findReadyThumbnailsByPromptIds(promptIds)) {
            result.computeIfAbsent(
                    image.getPromptId(),
                    ignored -> presignPromptImageDownloadPort.presignGet(image.getWatermarkedObjectKey())
            );
        }
        return result;
    }

    private Map<Long, List<TagInfo>> loadTags(List<Long> promptIds) {
        if (promptIds.isEmpty()) {
            return Map.of();
        }
        List<Object[]> rows = entityManager.createQuery("""
                select postTag.post.id, tag.id, tag.tagType, tag.tagName
                from PostTagJpaEntity postTag
                join postTag.tag tag
                where postTag.post.id in :promptIds
                order by postTag.post.id asc, tag.tagType asc, tag.id asc
                """, Object[].class)
                .setParameter("promptIds", promptIds)
                .getResultList();

        Map<Long, List<TagInfo>> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            result.computeIfAbsent((Long) row[0], ignored -> new ArrayList<>())
                    .add(new TagInfo((Long) row[1], (TagType) row[2], (String) row[3]));
        }
        return result;
    }

    private BookmarkPromptInfo toInfo(
            Object[] row,
            Map<Long, String> thumbnails,
            Map<Long, List<TagInfo>> tags
    ) {
        Long promptId = (Long) row[0];
        return new BookmarkPromptInfo(
                promptId,
                (String) row[1],
                thumbnails.get(promptId),
                (PromptContentType) row[2],
                (PromptOutputType) row[3],
                ((Number) row[4]).longValue(),
                ((Number) row[5]).longValue(),
                ((Number) row[6]).longValue(),
                new AuthorInfo((Long) row[7], (String) row[8], (String) row[9]),
                tags.getOrDefault(promptId, List.of()),
                (Instant) row[10]
        );
    }
}
