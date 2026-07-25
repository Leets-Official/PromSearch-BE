package com.promsearch.prompt.application;

/*
 * 홈 목록 조회에 필요한 입력 값을 application 계층 전용 query로 묶습니다.
 * Controller의 RequestParam이나 PathVariable을 service로 직접 넘기지 않고 이 record로 변환하면,
 * 이후 홈 목록 조회 조건이 늘어나도 interfaces 계층과 application 계층의 경계를 유지하기 쉽습니다.
 */
public record HomePromptListQuery(
        /*
         * 비회원 조회에서는 null입니다.
         * 값이 있으면 현재 사용자가 각 프롬프트를 좋아요/북마크했는지 함께 조회합니다.
         */
        Long viewerUserId,

        /*
         * 인기 목록에서는 사용하지 않으므로 null입니다.
         * 직군 목록에서만 JOB 타입 태그 ID로 사용합니다.
         */
        Long jobTagId,

        /*
         * Spring Pageable에 직접 의존하지 않기 위해 원시 page/size 값만 application 계층으로 넘깁니다.
         * 검증은 Controller의 Bean Validation에서 먼저 처리합니다.
         */
        int page,
        int size
) {

    public static HomePromptListQuery popular(Long viewerUserId, int page, int size) {
        // 인기 목록은 직군 태그 조건이 없으므로 jobTagId를 null로 고정합니다.
        return new HomePromptListQuery(viewerUserId, null, page, size);
    }

    public static HomePromptListQuery job(Long viewerUserId, Long jobTagId, int page, int size) {
        // 직군 목록은 특정 JOB 태그 ID를 기준으로 필터링합니다.
        return new HomePromptListQuery(viewerUserId, jobTagId, page, size);
    }
}
