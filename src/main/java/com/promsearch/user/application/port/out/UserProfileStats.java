package com.promsearch.user.application.port.out;

/*
 * 상대 프로필에 표시할 프롬프트 기반 누적 통계입니다.
 * user 도메인 상태가 아니라 prompt 도메인의 집계 결과이므로 outbound port 패키지에 둡니다.
 */
public record UserProfileStats(
        long promptCount,
        long totalLikeCount,
        long totalViewCount
) {

    public static UserProfileStats empty() {
        // 공개 가능한 프롬프트가 없을 때 화면에서 사용할 기본 통계입니다.
        return new UserProfileStats(0, 0, 0);
    }
}
