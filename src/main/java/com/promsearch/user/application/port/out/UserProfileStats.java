package com.promsearch.user.application.port.out;

public record UserProfileStats(
        long promptCount,
        long totalLikeCount,
        long totalViewCount
) {

    public static UserProfileStats empty() {
        return new UserProfileStats(0, 0, 0);
    }
}
