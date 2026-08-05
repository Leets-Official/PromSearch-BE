package com.promsearch.user.application.usecase.dto;

import com.promsearch.user.domain.enums.GradeRequestStatus;

public record GradeRequestListQuery(GradeRequestStatus status, String q, int page, int size) {

    public static final int MAX_SIZE = 100;
    public static final int MAX_PAGE = 1_000;

    public GradeRequestListQuery {
        q = normalize(q);
        validatePaging(page, size);
    }

    private static String normalize(String q) {
        return q == null || q.isBlank() ? null : q.strip();
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
        if ((long) page * size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("page offset is too large");
        }
    }
}
