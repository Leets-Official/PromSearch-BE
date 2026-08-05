package com.promsearch.community.interfaces;

import com.promsearch.community.application.usecase.BookmarkPromptUseCase;
import com.promsearch.community.application.usecase.ListBookmarksUseCase;
import com.promsearch.community.application.usecase.UnbookmarkPromptUseCase;
import com.promsearch.community.application.usecase.dto.BookmarkListQuery;
import com.promsearch.community.application.usecase.dto.BookmarkPromptCommand;
import com.promsearch.community.interfaces.docs.BookmarkControllerDocs;
import com.promsearch.community.interfaces.dto.response.BookmarkListResponse;
import com.promsearch.community.interfaces.dto.response.BookmarkResponse;
import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.response.code.SuccessCode;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class BookmarkController implements BookmarkControllerDocs {

    private final BookmarkPromptUseCase bookmarkPromptUseCase;
    private final UnbookmarkPromptUseCase unbookmarkPromptUseCase;
    private final ListBookmarksUseCase listBookmarksUseCase;

    @PostMapping("/prompts/{promptId}/bookmarks")
    @Override
    public ResponseEntity<ApiResponse<BookmarkResponse>> bookmark(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @PathVariable Long promptId
    ) {
        BookmarkResponse response = BookmarkResponse.from(bookmarkPromptUseCase.bookmark(
                new BookmarkPromptCommand(user.userId(), promptId)
        ));
        return ResponseEntity
                .status(SuccessCode.CREATED.getHttpStatus())
                .body(ApiResponse.onSuccess(SuccessCode.CREATED, response));
    }

    @DeleteMapping("/prompts/{promptId}/bookmarks")
    @Override
    public ApiResponse<BookmarkResponse> unbookmark(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @PathVariable Long promptId
    ) {
        return ApiResponse.onSuccess(BookmarkResponse.from(unbookmarkPromptUseCase.unbookmark(
                new BookmarkPromptCommand(user.userId(), promptId)
        )));
    }

    @GetMapping("/users/me/bookmarks")
    @Override
    public ApiResponse<BookmarkListResponse> list(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            @RequestParam(required = false) List<Long> taskTagIds,
            @RequestParam(required = false) List<Long> aiModelTagIds,
            @RequestParam(required = false) List<PromptOutputType> outputTypes,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ApiResponse.onSuccess(BookmarkListResponse.from(listBookmarksUseCase.list(
                new BookmarkListQuery(
                        user.userId(),
                        taskTagIds,
                        aiModelTagIds,
                        outputTypes,
                        page,
                        size
                )
        )));
    }
}
