package com.promsearch.prompt.infrastructure.persistence;

import com.promsearch.community.domain.enums.InteractionType;
import com.promsearch.prompt.application.port.out.prompt.HomePromptReader;
import com.promsearch.prompt.application.usecase.dto.HomePromptAuthorInfo;
import com.promsearch.prompt.application.usecase.dto.HomePromptListInfo;
import com.promsearch.prompt.application.usecase.dto.HomePromptListQuery;
import com.promsearch.prompt.application.usecase.dto.HomePromptStatisticsInfo;
import com.promsearch.prompt.application.usecase.dto.HomePromptSummaryInfo;
import com.promsearch.prompt.application.usecase.dto.HomePromptTagInfo;
import com.promsearch.prompt.application.usecase.dto.HomePromptViewerInteractionInfo;
import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.domain.enums.TagType;
import com.promsearch.user.domain.enums.UserStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
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
     * 홈 화면은 비회원도 접근할 수 있는 공개 영역입니다.
     * 노출 가능한 콘텐츠 타입을 명시적으로 제한해, 이후 enum 값이 추가되더라도
     * 의도하지 않은 콘텐츠가 홈 목록에 섞이지 않도록 합니다.
     */
    private static final Set<PromptContentType> HOME_CONTENT_TYPES = EnumSet.of(
            PromptContentType.FREE,
            PromptContentType.PREMIUM
    );

    /*
     * 인기 목록과 직군 목록은 카드에 필요한 projection 컬럼이 같습니다.
     * select 절을 상수로 분리해 두 목록 간 응답 필드가 어긋나는 일을 줄입니다.
     */
    private static final String CARD_SELECT = """
            select p.id, p.title, p.thumbnailImageUrl, p.outputType, p.contentType, p.pricePoint,
                   p.createdAt, u.id, u.nickname, u.profileImageUrl,
                   coalesce(s.viewCount, 0), coalesce(s.likeCount, 0),
                   coalesce(s.commentCount, 0), coalesce(s.copyCount, 0)
            """;

    /*
     * 홈 공개 목록의 공통 노출 조건입니다.
     * - ACTIVE 프롬프트만 노출
     * - 논리 삭제된 프롬프트 제외
     * - FREE/PREMIUM만 노출
     * - 작성자가 ACTIVE 상태인 경우만 노출
     *
     * 추후 PromptVisibility가 PostJpaEntity에 연결되면 이 조건에 PUBLIC 필터를 추가하면 됩니다.
     */
    private static final String PUBLIC_HOME_FILTER = """
            p.status = :activeStatus
            and p.deletedAt is null
            and p.contentType in :contentTypes
            and u.id = p.userId
            and u.status = :activeUserStatus
            """;

    private final EntityManager entityManager;

    @Override
    public HomePromptListInfo listPopularPrompts(HomePromptListQuery query) {
        /*
         * 5주차 회의록 기준으로 인기 프롬프트는 북마크 수가 아니라 좋아요 수로 정렬합니다.
         * 같은 좋아요 수에서는 생성 시각과 ID를 보조 정렬 기준으로 사용해 페이지네이션 결과가 흔들리지 않게 합니다.
         */
        List<PromptCardRow> rows = createCardQuery("""
                        from PostJpaEntity p
                        left join p.statistics s,
                             UserJpaEntity u
                        where """ + PUBLIC_HOME_FILTER + """
                        order by coalesce(s.likeCount, 0) desc, p.createdAt desc, p.id desc
                        """)
                .setFirstResult(toOffset(query.page(), query.size()))
                .setMaxResults(query.size())
                .getResultList()
                .stream()
                .map(PromptCardRow::from)
                .toList();

        long totalElements = setPublicHomeParameters(entityManager.createQuery("""
                        select count(p.id)
                        from PostJpaEntity p,
                             UserJpaEntity u
                        where """ + PUBLIC_HOME_FILTER, Long.class))
                .getSingleResult();

        return toListInfo(rows, query, totalElements);
    }

    @Override
    public HomePromptListInfo listJobPrompts(HomePromptListQuery query) {
        /*
         * 직군 섹션은 프롬프트 태그 중 JOB 타입만 기준으로 필터링합니다.
         * 태그명을 문자열로 비교하면 화면 언어나 표시명이 바뀔 때 조회 결과가 깨질 수 있습니다.
         */
        String jobFilter = PUBLIC_HOME_FILTER + """
                and t.id = :jobTagId
                and t.tagType = :jobTagType
                """;

        List<PromptCardRow> rows = setJobParameters(entityManager.createQuery(CARD_SELECT + """
                        from PostJpaEntity p
                        join p.postTags pt
                        join pt.tag t
                        left join p.statistics s,
                             UserJpaEntity u
                        where """ + jobFilter + """
                        order by p.createdAt desc, p.id desc
                        """, Object[].class), query.jobTagId())
                .setFirstResult(toOffset(query.page(), query.size()))
                .setMaxResults(query.size())
                .getResultList()
                .stream()
                .map(PromptCardRow::from)
                .toList();

        long totalElements = setJobParameters(entityManager.createQuery("""
                        select count(distinct p.id)
                        from PostJpaEntity p
                        join p.postTags pt
                        join pt.tag t,
                             UserJpaEntity u
                        where """ + jobFilter, Long.class), query.jobTagId())
                .getSingleResult();

        return toListInfo(rows, query, totalElements);
    }

    private TypedQuery<Object[]> createCardQuery(String fromWhereOrderClause) {
        /*
         * 인기 목록처럼 별도 태그 조인이 필요 없는 조회에서 사용하는 공통 카드 쿼리 생성 메서드입니다.
         * from/where/order 절만 외부에서 조립하고, select 절과 공개 홈 파라미터는 여기서 통일합니다.
         */
        return setPublicHomeParameters(entityManager.createQuery(CARD_SELECT + fromWhereOrderClause, Object[].class));
    }

    private <T> TypedQuery<T> setPublicHomeParameters(TypedQuery<T> typedQuery) {
        /*
         * 공개 홈 조회에 항상 들어가야 하는 파라미터를 한 곳에서 세팅합니다.
         * 목록 쿼리와 count 쿼리가 같은 조건을 쓰도록 만들어, 둘 사이의 조건 불일치를 방지합니다.
         */
        return typedQuery
                .setParameter("activeStatus", PromptStatus.ACTIVE)
                .setParameter("contentTypes", HOME_CONTENT_TYPES)
                .setParameter("activeUserStatus", UserStatus.ACTIVE);
    }

    private <T> TypedQuery<T> setJobParameters(TypedQuery<T> typedQuery, Long jobTagId) {
        /*
         * 직군 목록은 공개 홈 조건에 JOB 태그 조건이 추가됩니다.
         * tagType을 함께 제한해 다른 타입의 태그 ID가 들어와도 결과에 섞이지 않게 합니다.
         */
        return setPublicHomeParameters(typedQuery)
                .setParameter("jobTagId", jobTagId)
                .setParameter("jobTagType", TagType.JOB);
    }

    private HomePromptListInfo toListInfo(List<PromptCardRow> rows, HomePromptListQuery query, long totalElements) {
        List<Long> promptIds = rows.stream()
                .map(PromptCardRow::promptId)
                .toList();

        /*
         * 카드 이미지, 태그, 현재 사용자의 좋아요/북마크 여부는 페이지 조회 후 한 번씩 모아서 조회합니다.
         * 이렇게 하면 카드 응답은 풍부하게 유지하면서도 카드 개수만큼 쿼리가 늘어나는 문제를 피할 수 있습니다.
         */
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
            PromptCardRow row,
            Map<Long, String> thumbnailImages,
            Map<Long, List<HomePromptTagInfo>> tags,
            Map<Long, HomePromptViewerInteractionInfo> interactions
    ) {
        String thumbnailImageUrl = row.thumbnailImageUrl();
        if (thumbnailImageUrl == null || thumbnailImageUrl.isBlank()) {
            /*
             * posts.thumbnailImageUrl이 비어 있는 기존 데이터나 임시 데이터가 있을 수 있습니다.
             * 이 경우 post_images에서 thumbnail=true인 첫 이미지를 카드 썸네일로 대신 사용합니다.
             */
            thumbnailImageUrl = thumbnailImages.get(row.promptId());
        }

        return new HomePromptSummaryInfo(
                row.promptId(),
                row.title(),
                thumbnailImageUrl,
                row.outputType(),
                row.contentType(),
                row.pricePoint(),
                new HomePromptAuthorInfo(row.authorUserId(), row.authorNickname(), row.authorProfileImageUrl()),
                new HomePromptStatisticsInfo(
                        row.viewCount(),
                        row.likeCount(),
                        row.commentCount(),
                        row.copyCount()
                ),
                interactions.getOrDefault(row.promptId(), HomePromptViewerInteractionInfo.none()),
                tags.getOrDefault(row.promptId(), List.of()),
                row.createdAt()
        );
    }

    private Map<Long, String> findThumbnailImages(List<Long> promptIds) {
        if (promptIds.isEmpty()) {
            return Map.of();
        }

        /*
         * 프롬프트별 대표 이미지 후보를 한 번에 가져옵니다.
         * 같은 프롬프트에 thumbnail=true 이미지가 여러 개 있더라도 sortOrder/id가 가장 앞선 값만 사용합니다.
         */
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

        /*
         * 카드에 표시할 태그를 프롬프트 ID 묶음으로 한 번에 조회합니다.
         * 화면에서는 직군/작업/AI 모델 태그를 함께 보여줄 수 있으므로 특정 타입으로 제한하지 않습니다.
         */
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
            /*
             * 비회원 또는 조회 결과가 없는 경우에는 상호작용 조회를 생략합니다.
             * 응답 생성 시 기본값(HomePromptViewerInteractionInfo.none)을 사용하므로 liked/bookmarked는 false가 됩니다.
             */
            return Map.of();
        }

        /*
         * 현재 로그인 사용자가 이번 페이지의 프롬프트들에 누른 LIKE/BOOKMARK를 한 번에 조회합니다.
         * 이렇게 해야 카드마다 interaction 조회 쿼리가 반복되는 N+1 문제를 피할 수 있습니다.
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
        // page와 size는 Controller에서 검증되므로 여기서는 DB offset 계산만 담당합니다.
        return Math.toIntExact((long) page * size);
    }

    private static Long number(Object value) {
        // JPQL projection의 숫자 타입은 DB/방언에 따라 Integer, Long 등이 될 수 있어 Number로 안전하게 변환합니다.
        return ((Number) value).longValue();
    }

    private static long count(Object value) {
        // left join된 통계가 없을 수 있으므로 null 통계 값은 0으로 취급합니다.
        return value == null ? 0L : ((Number) value).longValue();
    }

    /*
     * JPQL projection 결과(Object[])를 바로 row[0], row[1]로 사용하면
     * 필드 의미를 추적하기 어렵고 select 절 변경 시 실수하기 쉽습니다.
     * 내부 record로 한 번 감싸 카드 생성 로직에서는 이름 있는 필드로 접근하게 합니다.
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
            long viewCount,
            long likeCount,
            long commentCount,
            long copyCount
    ) {

        private static PromptCardRow from(Object[] row) {
            /*
             * CARD_SELECT의 컬럼 순서와 1:1로 매핑됩니다.
             * select 절을 수정할 때는 이 매핑도 반드시 함께 확인해야 합니다.
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
                    count(row[10]),
                    count(row[11]),
                    count(row[12]),
                    count(row[13])
            );
        }
    }
}
