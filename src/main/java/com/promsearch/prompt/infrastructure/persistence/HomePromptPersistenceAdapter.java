package com.promsearch.prompt.infrastructure.persistence;

import com.promsearch.community.domain.enums.InteractionType;
import com.promsearch.prompt.application.port.out.prompt.HomePromptReader;
import com.promsearch.prompt.application.port.out.storage.PresignPromptImageDownloadPort;
import com.promsearch.prompt.application.usecase.dto.HomePromptAuthorInfo;
import com.promsearch.prompt.application.usecase.dto.HomePromptListInfo;
import com.promsearch.prompt.application.usecase.dto.HomePromptListQuery;
import com.promsearch.prompt.application.usecase.dto.HomePromptSort;
import com.promsearch.prompt.application.usecase.dto.HomePromptStatisticsInfo;
import com.promsearch.prompt.application.usecase.dto.HomePromptSummaryInfo;
import com.promsearch.prompt.application.usecase.dto.HomePromptTagInfo;
import com.promsearch.prompt.application.usecase.dto.HomePromptViewerInteractionInfo;
import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptImageStatus;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.domain.enums.PromptVisibility;
import com.promsearch.prompt.domain.enums.TagType;
import com.promsearch.user.application.port.out.profileimage.ProfileImageDeliveryPort;
import com.promsearch.user.domain.enums.UserStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class HomePromptPersistenceAdapter implements HomePromptReader {

    private static final char LIKE_ESCAPE = '\\';

    /*
     * 홈 화면은 비회원도 접근 가능한 공개 영역입니다.
     * FREE/PREMIUM만 노출해 추후 enum 값이 추가되더라도 의도하지 않은 콘텐츠가 섞이지 않게 합니다.
     */
    private static final Set<PromptContentType> HOME_CONTENT_TYPES = EnumSet.of(
            PromptContentType.FREE,
            PromptContentType.PREMIUM
    );

    /*
     * 홈 카드, 직군 카드, 필터 카드가 모두 같은 응답 모양을 사용하므로 projection을 한 곳에 둡니다.
     * select 절을 바꿀 때는 PromptCardRow.from의 컬럼 순서도 함께 확인해야 합니다.
     */
    private static final String CARD_SELECT = """
            select p.id, p.title, p.thumbnailImageUrl, p.outputType, p.contentType, p.pricePoint,
                   p.createdAt, u.id, u.nickname, u.profileImageUrl, u.profileImageObjectKey,
                   coalesce(s.viewCount, 0), coalesce(s.likeCount, 0),
                   coalesce(s.commentCount, 0), coalesce(s.copyCount, 0)
            """;

    /*
     * 홈 공개 목록의 공통 노출 조건입니다.
     * 작성자 비공개 글과 관리자/삭제 상태 글이 목록에 노출되지 않도록 서버에서 먼저 제한합니다.
     */
    private static final String PUBLIC_HOME_FILTER = """
            p.status = :activeStatus
            and p.visibility = :publicVisibility
            and p.deletedAt is null
            and p.contentType in :contentTypes
            and u.id = p.userId
            and u.status = :activeUserStatus
            and u.deletedAt is null
            """;

    private final EntityManager entityManager;
    private final PresignPromptImageDownloadPort presignPromptImageDownloadPort;
    private final ProfileImageDeliveryPort profileImageDeliveryPort;

    @Override
    public HomePromptListInfo listPrompts(HomePromptListQuery query) {
        return listByQuery(query);
    }

    @Override
    public HomePromptListInfo listPopularPrompts(HomePromptListQuery query) {
        return listByQuery(query);
    }

    @Override
    public HomePromptListInfo listJobPrompts(HomePromptListQuery query) {
        return listByQuery(query);
    }

    private HomePromptListInfo listByQuery(HomePromptListQuery query) {
        String filters = buildFilters(query);
        /*
         * 목록 조회와 count 조회가 같은 필터 문자열과 같은 파라미터를 공유합니다.
         * 한쪽만 조건이 바뀌어 페이지 정보(totalElements, hasNext)가 실제 목록과 어긋나는 일을 막기 위한 구조입니다.
         */
        TypedQuery<Object[]> contentQuery = entityManager.createQuery(CARD_SELECT + """
                        from PostJpaEntity p
                        left join p.statistics s,
                             UserJpaEntity u
                        where
                        """ + PUBLIC_HOME_FILTER + filters + orderBy(query.sort()), Object[].class);
        applyParameters(contentQuery, query);

        List<PromptCardRow> rows = contentQuery
                .setFirstResult(toOffset(query.page(), query.size()))
                .setMaxResults(query.size())
                .getResultList()
                .stream()
                .map(PromptCardRow::from)
                .toList();

        TypedQuery<Long> countQuery = entityManager.createQuery("""
                        select count(p.id)
                        from PostJpaEntity p,
                             UserJpaEntity u
                        where
                        """ + PUBLIC_HOME_FILTER + filters, Long.class);
        applyParameters(countQuery, query);

        return toListInfo(rows, query, countQuery.getSingleResult());
    }

    private String buildFilters(HomePromptListQuery query) {
        StringBuilder filters = new StringBuilder();
        if (query.jobTagId() != null) {
            /*
             * 직군 필터는 tags.id만 비교하지 않고 tagType까지 함께 제한합니다.
             * 같은 숫자 ID가 다른 타입으로 잘못 전달되더라도 JOB 태그가 아니면 결과에 포함되지 않습니다.
             */
            filters.append("""
                    and exists (
                        select postTag.id
                        from PostTagJpaEntity postTag
                        where postTag.post.id = p.id
                          and postTag.tag.id = :jobTagId
                          and postTag.tag.tagType = :jobTagType
                    )
                    """);
        }
        if (!query.taskTagIds().isEmpty()) {
            /*
             * 태스크 체크박스는 여러 개를 동시에 선택할 수 있습니다.
             * 사용자는 "PPT 또는 레포트"처럼 넓혀서 찾는 흐름을 기대하므로 in 조건으로 OR 의미를 적용합니다.
             */
            filters.append("""
                    and exists (
                        select postTag.id
                        from PostTagJpaEntity postTag
                        where postTag.post.id = p.id
                          and postTag.tag.id in :taskTagIds
                          and postTag.tag.tagType = :taskTagType
                    )
                    """);
        }
        if (query.aiModelTagId() != null) {
            /*
             * AI 모델은 업로드 시 기존 AI_MODEL 태그 또는 custom AI 모델 태그와 연결됩니다.
             * 필터 조회에서는 새 태그를 만들지 않고, 이미 연결된 AI_MODEL 태그 ID만 조건으로 사용합니다.
             */
            filters.append("""
                    and exists (
                        select postTag.id
                        from PostTagJpaEntity postTag
                        where postTag.post.id = p.id
                          and postTag.tag.id = :aiModelTagId
                          and postTag.tag.tagType = :aiModelTagType
                    )
                    """);
        }
        if (query.outputType() != null) {
            filters.append("""
                    and p.outputType = :outputType
                    """);
        }
        if (query.hasKeyword()) {
            /*
             * 검색창은 카드에서 사용자가 바로 확인할 수 있는 제목, 설명, 태그명을 대상으로 합니다.
             * 본문(promptBody)은 유료 콘텐츠 원문 노출 정책과 검색 비용을 고려해 홈 목록 검색 범위에서 제외합니다.
             */
            filters.append("""
                    and (
                        lower(p.title) like :keyword escape '\\'
                        or lower(coalesce(p.description, '')) like :keyword escape '\\'
                        or exists (
                            select keywordPostTag.id
                            from PostTagJpaEntity keywordPostTag
                            where keywordPostTag.post.id = p.id
                              and lower(keywordPostTag.tag.tagName) like :keyword escape '\\'
                        )
                    )
                    """);
        }
        return filters.toString();
    }

    private String orderBy(HomePromptSort sort) {
        if (sort == HomePromptSort.POPULAR) {
            /*
             * 5주차 회의록 기준 인기 정렬은 북마크가 아니라 좋아요 수입니다.
             * createdAt과 id를 보조 정렬로 둬 같은 좋아요 수에서도 페이지 결과가 흔들리지 않게 합니다.
             */
            return """
                    order by coalesce(s.likeCount, 0) desc, p.createdAt desc, p.id desc
                    """;
        }
        return """
                order by p.createdAt desc, p.id desc
                """;
    }

    private void applyParameters(TypedQuery<?> typedQuery, HomePromptListQuery query) {
        typedQuery
                .setParameter("activeStatus", PromptStatus.ACTIVE)
                .setParameter("publicVisibility", PromptVisibility.PUBLIC)
                .setParameter("contentTypes", HOME_CONTENT_TYPES)
                .setParameter("activeUserStatus", UserStatus.ACTIVE);

        if (query.jobTagId() != null) {
            typedQuery
                    .setParameter("jobTagId", query.jobTagId())
                    .setParameter("jobTagType", TagType.JOB);
        }
        if (!query.taskTagIds().isEmpty()) {
            typedQuery
                    .setParameter("taskTagIds", query.taskTagIds())
                    .setParameter("taskTagType", TagType.TASK);
        }
        if (query.aiModelTagId() != null) {
            typedQuery
                    .setParameter("aiModelTagId", query.aiModelTagId())
                    .setParameter("aiModelTagType", TagType.AI_MODEL);
        }
        if (query.outputType() != null) {
            typedQuery.setParameter("outputType", query.outputType());
        }
        if (query.hasKeyword()) {
            /*
             * DB lower(...) 비교와 맞추기 위해 애플리케이션에서도 소문자로 변환합니다.
             * 사용자가 입력한 %, _, \ 문자가 LIKE 와일드카드로 해석되지 않도록 escape한 뒤 포함 검색 패턴을 만듭니다.
             */
            typedQuery.setParameter("keyword", toEscapedContainsKeyword(query.keyword()));
        }
    }

    private HomePromptListInfo toListInfo(List<PromptCardRow> rows, HomePromptListQuery query, long totalElements) {
        List<Long> promptIds = rows.stream()
                .map(PromptCardRow::promptId)
                .toList();

        /*
         * 이미지, 태그, 현재 사용자의 좋아요/북마크 여부는 페이지 결과를 기준으로 한 번씩 모아서 조회합니다.
         * 카드 개수만큼 추가 쿼리가 반복되는 N+1 문제를 막기 위한 배치 조회입니다.
         */
        Map<Long, String> thumbnailImages = findThumbnailImages(promptIds);
        Map<Long, List<HomePromptTagInfo>> tags = findTags(promptIds);
        Map<Long, List<String>> customAiModels = findCustomAiModels(promptIds);
        Map<Long, HomePromptViewerInteractionInfo> interactions = findViewerInteractions(query.viewerUserId(), promptIds);

        List<HomePromptSummaryInfo> prompts = rows.stream()
                .map(row -> toSummaryInfo(row, thumbnailImages, tags, customAiModels, interactions))
                .toList();

        boolean hasNext = ((long) query.page() + 1) * query.size() < totalElements;
        return new HomePromptListInfo(prompts, query.page(), query.size(), totalElements, hasNext);
    }

    private HomePromptSummaryInfo toSummaryInfo(
            PromptCardRow row,
            Map<Long, String> thumbnailImages,
            Map<Long, List<HomePromptTagInfo>> tags,
            Map<Long, List<String>> customAiModels,
            Map<Long, HomePromptViewerInteractionInfo> interactions
    ) {
        /*
         * 최신 이미지 모델에서는 READY 워터마크 이미지를 우선 사용합니다.
         * 기존 데이터에만 남아 있을 수 있는 posts.thumbnailImageUrl은 마지막 대체값입니다.
         */
        String thumbnailImageUrl = thumbnailImages.get(row.promptId());
        if (thumbnailImageUrl == null || thumbnailImageUrl.isBlank()) {
            thumbnailImageUrl = row.thumbnailImageUrl();
        }

        return new HomePromptSummaryInfo(
                row.promptId(),
                row.title(),
                thumbnailImageUrl,
                row.outputType(),
                row.contentType(),
                row.pricePoint(),
                new HomePromptAuthorInfo(
                        row.authorUserId(),
                        row.authorNickname(),
                        profileImageDeliveryPort.resolve(
                                row.authorProfileImageUrl(),
                                row.authorProfileImageObjectKey()
                        )
                ),
                new HomePromptStatisticsInfo(
                        row.viewCount(),
                        row.likeCount(),
                        row.commentCount(),
                        row.copyCount()
                ),
                interactions.getOrDefault(row.promptId(), HomePromptViewerInteractionInfo.none()),
                tags.getOrDefault(row.promptId(), List.of()),
                customAiModels.getOrDefault(row.promptId(), List.of()),
                row.createdAt()
        );
    }

    private Map<Long, String> findThumbnailImages(List<Long> promptIds) {
        if (promptIds.isEmpty()) {
            return Map.of();
        }

        /*
         * 홈 카드에는 원본 이미지가 아니라 워터마크 처리가 끝난 READY 이미지만 노출합니다.
         * 이미지가 여러 장이면 thumbnail=true 중 sortOrder가 가장 빠른 이미지를 대표 썸네일로 사용합니다.
         */
        List<Object[]> rows = entityManager.createQuery("""
                        select image.promptId, image.watermarkedObjectKey
                        from PromptImageJpaEntity image
                        where image.promptId in :promptIds
                          and image.thumbnail = true
                          and image.status = :readyStatus
                          and image.deletedAt is null
                          and image.watermarkedObjectKey is not null
                        order by image.promptId asc, image.sortOrder asc, image.id asc
                        """, Object[].class)
                .setParameter("promptIds", promptIds)
                .setParameter("readyStatus", PromptImageStatus.READY)
                .getResultList();

        Map<Long, String> thumbnails = new HashMap<>();
        for (Object[] row : rows) {
            Long promptId = number(row[0]);
            String objectKey = (String) row[1];
            /*
             * Object Key를 그대로 내려주지 않고 짧은 수명의 Presigned URL로 변환합니다.
             * 같은 프롬프트에 후보 이미지가 여러 개 있어도 첫 번째 대표 이미지만 사용합니다.
             */
            thumbnails.computeIfAbsent(promptId, ignored -> presignPromptImageDownloadPort.presignGet(objectKey));
        }
        return thumbnails;
    }

    private Map<Long, List<HomePromptTagInfo>> findTags(List<Long> promptIds) {
        if (promptIds.isEmpty()) {
            return Map.of();
        }

        /*
         * 카드 태그는 직군, 태스크, AI 모델을 함께 표시할 수 있으므로 타입을 제한하지 않습니다.
         * 프롬프트 ID 묶음으로 한 번에 조회해 카드마다 태그 쿼리가 반복되는 일을 피합니다.
         */
        List<Object[]> rows = entityManager.createQuery("""
                        select p.id, tag.id, tag.tagType, tag.tagName
                        from PostJpaEntity p
                        join p.postTags pt
                        join pt.tag tag
                        where p.id in :promptIds
                          and (tag.custom = false or tag.custom is null)
                        order by p.id asc, tag.id asc
                        """, Object[].class)
                .setParameter("promptIds", promptIds)
                .getResultList();

        Map<Long, List<HomePromptTagInfo>> tags = new LinkedHashMap<>();
        for (Object[] row : rows) {
            tags.computeIfAbsent(number(row[0]), ignored -> new ArrayList<>())
                    .add(new HomePromptTagInfo(number(row[1]), (TagType) row[2], (String) row[3]));
        }
        /*
         * DB enum 정렬은 구현체와 저장 방식에 따라 달라질 수 있습니다.
         * 홈 카드에서는 직군 → 작업 유형 → AI 모델 순서가 가장 읽기 쉬우므로 응답 직전에 명시적으로 맞춥니다.
         */
        tags.values().forEach(values -> values.sort(
                Comparator.comparingInt((HomePromptTagInfo tag) -> tagSortOrder(tag.tagType()))
                        .thenComparing(HomePromptTagInfo::tagId)
        ));
        return tags;
    }

    private Map<Long, List<String>> findCustomAiModels(List<Long> promptIds) {
        if (promptIds.isEmpty()) {
            return Map.of();
        }

        List<Object[]> rows = entityManager.createQuery("""
                        select p.id, tag.tagName
                        from PostJpaEntity p
                        join p.postTags pt
                        join pt.tag tag
                        where p.id in :promptIds
                          and tag.tagType = :tagType
                          and tag.custom = true
                        """, Object[].class)
                .setParameter("promptIds", promptIds)
                .setParameter("tagType", TagType.AI_MODEL)
                .getResultList();

        Map<Long, List<String>> customAiModels = new HashMap<>();
        for (Object[] row : rows) {
            customAiModels.computeIfAbsent(number(row[0]), ignored -> new ArrayList<>())
                    .add((String) row[1]);
        }
        return customAiModels;
    }

    private Map<Long, HomePromptViewerInteractionInfo> findViewerInteractions(Long viewerUserId, List<Long> promptIds) {
        if (viewerUserId == null || promptIds.isEmpty()) {
            return Map.of();
        }

        /*
         * liked/bookmarked는 로그인 사용자에게만 의미가 있는 개인화 필드입니다.
         * 현재 페이지의 프롬프트 ID만 모아서 조회해 불필요한 상호작용 조회 범위를 줄입니다.
         */
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

    private static Long number(Object value) {
        return ((Number) value).longValue();
    }

    private static int tagSortOrder(TagType tagType) {
        return switch (tagType) {
            case JOB -> 0;
            case TASK -> 1;
            case AI_MODEL -> 2;
        };
    }

    private static String toEscapedContainsKeyword(String keyword) {
        String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
        StringBuilder pattern = new StringBuilder(lowerKeyword.length() + 2)
                .append('%');
        for (int index = 0; index < lowerKeyword.length(); index++) {
            char character = lowerKeyword.charAt(index);
            if (character == LIKE_ESCAPE || character == '%' || character == '_') {
                pattern.append(LIKE_ESCAPE);
            }
            pattern.append(character);
        }
        return pattern.append('%').toString();
    }

    private static long count(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    /*
     * JPQL projection 결과를 바로 row[0], row[1]로 사용하면 select 절 변경 시 실수하기 쉽습니다.
     * 내부 record로 한 번 감싸 카드 응답 생성 로직에서는 이름 있는 필드로 다루게 합니다.
     */
    private record PromptCardRow(
            Long promptId,
            String title,
            String thumbnailImageUrl,
            PromptOutputType outputType,
            PromptContentType contentType,
            Long pricePoint,
            Instant createdAt,
            Long authorUserId,
            String authorNickname,
            String authorProfileImageUrl,
            String authorProfileImageObjectKey,
            long viewCount,
            long likeCount,
            long commentCount,
            long copyCount
    ) {

        private static PromptCardRow from(Object[] row) {
            /*
             * CARD_SELECT의 컬럼 순서와 1:1로 매핑됩니다.
             * select 절에 컬럼을 추가하거나 순서를 바꾸면 이 매핑도 반드시 함께 수정해야 합니다.
             */
            return new PromptCardRow(
                    number(row[0]),
                    (String) row[1],
                    (String) row[2],
                    (PromptOutputType) row[3],
                    (PromptContentType) row[4],
                    count(row[5]),
                    (Instant) row[6],
                    number(row[7]),
                    (String) row[8],
                    (String) row[9],
                    (String) row[10],
                    count(row[11]),
                    count(row[12]),
                    count(row[13]),
                    count(row[14])
            );
        }
    }
}
