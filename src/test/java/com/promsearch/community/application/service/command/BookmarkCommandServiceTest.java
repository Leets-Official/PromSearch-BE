package com.promsearch.community.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.promsearch.community.application.port.out.postinteraction.CreatePostInteractionPort;
import com.promsearch.community.application.port.out.postinteraction.DeletePostInteractionIfPresentPort;
import com.promsearch.community.application.port.out.prompt.EnsurePromptInteractionTargetPort;
import com.promsearch.community.application.usecase.dto.BookmarkInfo;
import com.promsearch.community.application.usecase.dto.BookmarkPromptCommand;
import com.promsearch.community.domain.PostInteraction;
import com.promsearch.community.domain.enums.InteractionType;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookmarkCommandServiceTest {

    @Mock
    private CreatePostInteractionPort createPostInteractionPort;
    @Mock
    private DeletePostInteractionIfPresentPort deletePostInteractionIfPresentPort;
    @Mock
    private EnsurePromptInteractionTargetPort ensurePromptInteractionTargetPort;

    private BookmarkCommandService service;

    @BeforeEach
    void setUp() {
        service = new BookmarkCommandService(
                createPostInteractionPort,
                deletePostInteractionIfPresentPort,
                ensurePromptInteractionTargetPort
        );
    }

    @DisplayName("북마크 가능 프롬프트를 검증한 뒤 BOOKMARK 상호작용을 저장한다")
    @Test
    void bookmark() {
        Instant bookmarkedAt = Instant.parse("2026-07-13T14:00:00Z");
        when(createPostInteractionPort.create(org.mockito.ArgumentMatchers.any()))
                .thenReturn(PostInteraction.reconstruct(
                        new PostInteraction.PostInteractionId(1L),
                        2L,
                        10L,
                        InteractionType.BOOKMARK,
                        bookmarkedAt
                ));

        BookmarkInfo info = service.bookmark(new BookmarkPromptCommand(2L, 10L));

        ArgumentCaptor<PostInteraction> captor = ArgumentCaptor.forClass(PostInteraction.class);
        verify(createPostInteractionPort).create(captor.capture());
        assertThat(captor.getValue().getInteractionType()).isEqualTo(InteractionType.BOOKMARK);
        assertThat(info).isEqualTo(new BookmarkInfo(true, bookmarkedAt));

        InOrder order = inOrder(ensurePromptInteractionTargetPort, createPostInteractionPort);
        order.verify(ensurePromptInteractionTargetPort).ensureActivePublicPrompt(10L);
        order.verify(createPostInteractionPort).create(org.mockito.ArgumentMatchers.any());
    }

    @DisplayName("북마크 취소는 기록이 없어도 성공하도록 멱등 삭제 포트를 사용한다")
    @Test
    void unbookmarkIsIdempotent() {
        BookmarkInfo info = service.unbookmark(new BookmarkPromptCommand(2L, 10L));

        assertThat(info).isEqualTo(new BookmarkInfo(false, null));
        verify(deletePostInteractionIfPresentPort)
                .deleteIfPresent(2L, 10L, InteractionType.BOOKMARK);
    }
}
