package com.promsearch.community.application.service.query;

import com.promsearch.community.application.port.out.bookmark.LoadBookmarkListPort;
import com.promsearch.community.application.usecase.ListBookmarksUseCase;
import com.promsearch.community.application.usecase.dto.BookmarkListInfo;
import com.promsearch.community.application.usecase.dto.BookmarkListQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkQueryService implements ListBookmarksUseCase {

    private final LoadBookmarkListPort loadBookmarkListPort;

    @Override
    public BookmarkListInfo list(BookmarkListQuery query) {
        return loadBookmarkListPort.load(query);
    }
}
