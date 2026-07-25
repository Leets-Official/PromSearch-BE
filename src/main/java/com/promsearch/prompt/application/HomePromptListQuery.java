package com.promsearch.prompt.application;

public record HomePromptListQuery(
        Long viewerUserId,
        Long jobTagId,
        int page,
        int size
) {

    public static HomePromptListQuery popular(Long viewerUserId, int page, int size) {
        return new HomePromptListQuery(viewerUserId, null, page, size);
    }

    public static HomePromptListQuery job(Long viewerUserId, Long jobTagId, int page, int size) {
        return new HomePromptListQuery(viewerUserId, jobTagId, page, size);
    }
}
