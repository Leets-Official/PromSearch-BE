package com.promsearch.community.application.usecase;

import com.promsearch.community.application.usecase.dto.BookmarkInfo;
import com.promsearch.community.application.usecase.dto.BookmarkPromptCommand;

public interface UnbookmarkPromptUseCase {

    BookmarkInfo unbookmark(BookmarkPromptCommand command);
}
