package com.promsearch.community.application.port.out.bookmark;

import com.promsearch.community.application.usecase.dto.BookmarkListInfo;
import com.promsearch.community.application.usecase.dto.BookmarkListQuery;

public interface LoadBookmarkListPort {

    BookmarkListInfo load(BookmarkListQuery query);
}
