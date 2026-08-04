package com.promsearch.prompt.application.usecase.dto;

import com.promsearch.prompt.domain.enums.PromptOutputType;
import java.util.LinkedHashSet;
import java.util.List;

/*
 * 홈 목록 조회에 필요한 조건을 application 계층 전용 query로 묶습니다.
 * Controller의 RequestParam을 서비스로 직접 넘기지 않고 이 record로 변환해,
 * 화면 필터가 늘어나도 interfaces 계층과 application 계층의 경계를 유지합니다.
 *
 * 이 객체는 단순 값 전달자 역할만 하지 않고, 필터 ID 중복 제거와 검색어 공백 정리처럼
 * 모든 홈 목록 진입점에 공통으로 필요한 최소한의 입력 정규화도 함께 담당합니다.
 */
public record HomePromptListQuery(
        /*
         * 비회원 조회에서는 null입니다.
         * 로그인 사용자인 경우 liked/bookmarked 같은 개인화 카드 상태 조회에 사용합니다.
         */
        Long viewerUserId,

        /*
         * 좌측 직군 메뉴에서 선택한 JOB 타입 태그 ID입니다.
         * 선택하지 않으면 전체 직군을 의미하므로 null입니다.
         */
        Long jobTagId,

        /*
         * 화면의 태스크 다중 선택 필터입니다.
         * 여러 개를 선택하면 그중 하나 이상을 가진 프롬프트를 조회합니다.
         */
        List<Long> taskTagIds,

        /*
         * AI 모델 드롭다운에서 선택한 AI_MODEL 타입 태그 ID입니다.
         * 전체 옵션이면 null입니다.
         */
        Long aiModelTagId,

        /*
         * 결과물 전체/TEXT/IMAGE 필터입니다.
         * 전체 옵션이면 null입니다.
         */
        PromptOutputType outputType,

        /*
         * 검색창 입력값입니다. 제목, 설명, 태그명에 대해 부분 검색합니다.
         */
        String keyword,

        /*
         * 홈 기본 목록은 최신순, 인기 프롬프트는 좋아요순으로 정렬합니다.
         */
        HomePromptSort sort,

        /*
         * Spring Pageable에 직접 의존하지 않기 위해 원시 page/size 값만 application 계층으로 넘깁니다.
         * Controller 검증을 우선 적용하되, application 경계에서도 offset overflow를 방어합니다.
         */
        int page,
        int size
) {

    public static final int MAX_SIZE = 50;
    public static final int MAX_PAGE = 1_000;
    public static final int MAX_FILTER_TAGS = 10;
    public static final int MIN_KEYWORD_LENGTH = 2;
    public static final int MAX_KEYWORD_LENGTH = 100;

    public HomePromptListQuery {
        /*
         * Controller 검증을 우회하는 테스트나 내부 호출이 생겨도 application 경계에서 같은 규칙을 적용합니다.
         * 특히 taskTagIds는 반복 query parameter로 들어와 같은 값이 중복될 수 있어 여기서 순서를 유지한 채 제거합니다.
         */
        taskTagIds = normalizeTagIds(taskTagIds);
        keyword = normalizeKeyword(keyword);
        sort = sort == null ? HomePromptSort.LATEST : sort;

        validatePositiveTagId(jobTagId, "jobTagId");
        validatePositiveTagId(aiModelTagId, "aiModelTagId");
        validatePaging(page, size);
    }

    public static HomePromptListQuery filtered(
            Long viewerUserId,
            Long jobTagId,
            List<Long> taskTagIds,
            Long aiModelTagId,
            PromptOutputType outputType,
            String keyword,
            HomePromptSort sort,
            int page,
            int size
    ) {
        return new HomePromptListQuery(
                viewerUserId,
                jobTagId,
                taskTagIds,
                aiModelTagId,
                outputType,
                keyword,
                sort,
                page,
                size
        );
    }

    public static HomePromptListQuery popular(Long viewerUserId, int page, int size) {
        /*
         * 인기 목록은 필터가 없고 정렬만 좋아요순으로 고정된 홈 목록의 특수 케이스입니다.
         * 같은 record를 사용해 응답 조립과 페이지 검증 규칙을 필터 목록과 공유합니다.
         */
        return filtered(viewerUserId, null, List.of(), null, null, null, HomePromptSort.POPULAR, page, size);
    }

    public static HomePromptListQuery job(Long viewerUserId, Long jobTagId, int page, int size) {
        /*
         * 기존 직군별 API 호환을 위해 남겨둔 진입점입니다.
         * 새 홈 필터 API와 같은 조회 엔진을 쓰되 JOB 태그만 미리 채워 넣습니다.
         */
        return filtered(viewerUserId, jobTagId, List.of(), null, null, null, HomePromptSort.LATEST, page, size);
    }

    public boolean hasKeyword() {
        return keyword != null;
    }

    private static List<Long> normalizeTagIds(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> uniqueTagIds = new LinkedHashSet<>();
        for (Long tagId : tagIds) {
            validatePositiveTagId(tagId, "taskTagIds");
            uniqueTagIds.add(tagId);
        }
        if (uniqueTagIds.size() > MAX_FILTER_TAGS) {
            throw new IllegalArgumentException("taskTagIds size must be " + MAX_FILTER_TAGS + " or less");
        }
        return List.copyOf(uniqueTagIds);
    }

    private static String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        /*
         * 공백만 있는 검색어는 필터 없음과 같게 처리합니다.
         * 의미 있는 검색어만 persistence 계층으로 내려보내 불필요한 like 조건 생성을 막습니다.
         */
        String trimmed = keyword.trim();
        if (trimmed.length() < MIN_KEYWORD_LENGTH) {
            throw new IllegalArgumentException("keyword must be at least " + MIN_KEYWORD_LENGTH + " characters");
        }
        if (trimmed.length() > MAX_KEYWORD_LENGTH) {
            throw new IllegalArgumentException("keyword must be " + MAX_KEYWORD_LENGTH + " characters or less");
        }
        return trimmed;
    }

    private static void validatePositiveTagId(Long tagId, String fieldName) {
        if (tagId != null && tagId <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than 0");
        }
    }

    private static void validatePaging(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be 0 or greater");
        }
        if (page > MAX_PAGE) {
            throw new IllegalArgumentException("page must be " + MAX_PAGE + " or less");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_SIZE);
        }
        /*
         * offset pagination은 page가 커질수록 DB가 건너뛰어야 하는 row가 늘어납니다.
         * 공개 홈 API가 과도한 page 요청으로 느려지지 않도록 현재 서비스 규모에 맞는 상한을 둡니다.
         */
        if ((long) page * size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("page offset is too large");
        }
    }
}
