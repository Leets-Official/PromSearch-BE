package com.promsearch.community.interfaces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.promsearch.auth.application.usecase.AuthenticateAccessTokenUseCase;
import com.promsearch.community.application.usecase.BookmarkPromptUseCase;
import com.promsearch.community.application.usecase.ListBookmarksUseCase;
import com.promsearch.community.application.usecase.UnbookmarkPromptUseCase;
import com.promsearch.community.application.usecase.dto.BookmarkInfo;
import com.promsearch.community.application.usecase.dto.BookmarkListInfo;
import com.promsearch.community.application.usecase.dto.BookmarkListQuery;
import com.promsearch.community.application.usecase.dto.BookmarkPromptCommand;
import com.promsearch.global.security.AuthenticatedUserPrincipal;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BookmarkController.class)
@AutoConfigureMockMvc(addFilters = false)
class BookmarkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticateAccessTokenUseCase authenticateAccessTokenUseCase;

    @MockitoBean
    private BookmarkPromptUseCase bookmarkPromptUseCase;

    @MockitoBean
    private UnbookmarkPromptUseCase unbookmarkPromptUseCase;

    @MockitoBean
    private ListBookmarksUseCase listBookmarksUseCase;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @DisplayName("북마크 등록은 201과 등록 시각을 반환한다")
    @Test
    void bookmark() throws Exception {
        Instant bookmarkedAt = Instant.parse("2026-07-13T14:00:00Z");
        Mockito.when(bookmarkPromptUseCase.bookmark(Mockito.any()))
                .thenReturn(new BookmarkInfo(true, bookmarkedAt));

        mockMvc.perform(post("/api/v1/prompts/{promptId}/bookmarks", 10L)
                        .with(request -> authenticated(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("COMMON-201"))
                .andExpect(jsonPath("$.result.bookmarked").value(true))
                .andExpect(jsonPath("$.result.bookmarkedAt").value("2026-07-13T14:00:00Z"));

        ArgumentCaptor<BookmarkPromptCommand> captor =
                ArgumentCaptor.forClass(BookmarkPromptCommand.class);
        Mockito.verify(bookmarkPromptUseCase).bookmark(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new BookmarkPromptCommand(1L, 10L));
    }

    @DisplayName("북마크 취소 응답은 같은 DTO를 쓰되 등록 시각을 노출하지 않는다")
    @Test
    void unbookmark() throws Exception {
        Mockito.when(unbookmarkPromptUseCase.unbookmark(Mockito.any()))
                .thenReturn(new BookmarkInfo(false, null));

        mockMvc.perform(delete("/api/v1/prompts/{promptId}/bookmarks", 10L)
                        .with(request -> authenticated(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.bookmarked").value(false))
                .andExpect(jsonPath("$.result.bookmarkedAt").doesNotExist());
    }

    @DisplayName("내 북마크 목록의 세 필터와 페이지 정보를 조회 쿼리로 변환한다")
    @Test
    void listBookmarks() throws Exception {
        Mockito.when(listBookmarksUseCase.list(Mockito.any()))
                .thenReturn(new BookmarkListInfo(List.of(), 1, 6, 0, false));

        mockMvc.perform(get("/api/v1/users/me/bookmarks")
                        .param("taskTagId", "3")
                        .param("aiModelTagId", "7")
                        .param("outputType", "IMAGE")
                        .param("page", "1")
                        .with(request -> authenticated(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content").isArray())
                .andExpect(jsonPath("$.result.page").value(1))
                .andExpect(jsonPath("$.result.size").value(6));

        ArgumentCaptor<BookmarkListQuery> captor =
                ArgumentCaptor.forClass(BookmarkListQuery.class);
        Mockito.verify(listBookmarksUseCase).list(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new BookmarkListQuery(
                1L,
                3L,
                7L,
                PromptOutputType.IMAGE,
                1,
                6
        ));
    }

    @DisplayName("페이지 크기는 최대 50이어야 한다")
    @Test
    void sizeMustBeAtMostFifty() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/bookmarks")
                        .param("size", "51")
                        .with(request -> authenticated(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-400"));
    }

    private org.springframework.mock.web.MockHttpServletRequest authenticated(
            jakarta.servlet.http.HttpServletRequest request
    ) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new AuthenticatedUserPrincipal(1L, "USER"),
                        null
                )
        );
        return (org.springframework.mock.web.MockHttpServletRequest) request;
    }
}
