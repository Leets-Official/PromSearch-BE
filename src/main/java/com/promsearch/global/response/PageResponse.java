package com.promsearch.global.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "페이지네이션 목록 응답")
public record PageResponse<T>(
        @Schema(description = "현재 페이지 항목 목록")
        List<T> content,
        @Schema(description = "현재 페이지 번호(0부터 시작)", example = "0")
        int page,
        @Schema(description = "페이지당 항목 수", example = "20")
        int size,
        @Schema(description = "전체 항목 수", example = "42")
        long totalElements,
        @Schema(description = "전체 페이지 수", example = "3")
        int totalPages,
        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext
) {

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        boolean hasNext = (long) (page + 1) * size < totalElements;

        return new PageResponse<>(content, page, size, totalElements, totalPages, hasNext);
    }
}
