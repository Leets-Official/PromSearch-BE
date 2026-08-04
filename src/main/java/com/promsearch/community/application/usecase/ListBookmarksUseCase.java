package com.promsearch.community.application.usecase;

import com.promsearch.community.application.usecase.dto.BookmarkListInfo;
import com.promsearch.community.application.usecase.dto.BookmarkListQuery;

public interface ListBookmarksUseCase {

    BookmarkListInfo list(BookmarkListQuery query);
}
