package com.promsearch.community.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.promsearch.community.application.port.out.postinteraction.CreatePostInteractionPort;
import com.promsearch.community.application.port.out.postinteraction.DeletePostInteractionPort;
import com.promsearch.community.application.port.out.prompt.UpdatePromptLikeCountPort;
import com.promsearch.community.application.usecase.dto.LikeInfo;
import com.promsearch.community.application.usecase.dto.LikePromptCommand;
import com.promsearch.community.domain.PostInteraction;
import com.promsearch.community.domain.enums.InteractionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LikeCommandServiceTest {

    @Mock
    private CreatePostInteractionPort createPostInteractionPort;
    @Mock
    private DeletePostInteractionPort deletePostInteractionPort;
    @Mock
    private UpdatePromptLikeCountPort updatePromptLikeCountPort;

    private LikeCommandService service;

    @BeforeEach
    void setUp() {
        service = new LikeCommandService(
                createPostInteractionPort,
                deletePostInteractionPort,
                updatePromptLikeCountPort
        );
    }

    @DisplayName("좋아요를 등록하고 증가된 좋아요 수를 반환한다")
    @Test
    void like() {
        when(updatePromptLikeCountPort.increase(10L)).thenReturn(4L);

        LikeInfo info = service.like(new LikePromptCommand(1L, 10L));

        ArgumentCaptor<PostInteraction> interactionCaptor = ArgumentCaptor.forClass(PostInteraction.class);
        verify(createPostInteractionPort).create(interactionCaptor.capture());
        assertThat(interactionCaptor.getValue().getUserId()).isEqualTo(1L);
        assertThat(interactionCaptor.getValue().getPostId()).isEqualTo(10L);
        assertThat(interactionCaptor.getValue().getInteractionType()).isEqualTo(InteractionType.LIKE);
        assertThat(info).isEqualTo(new LikeInfo(10L, true, 4L));

        InOrder order = inOrder(createPostInteractionPort, updatePromptLikeCountPort);
        order.verify(createPostInteractionPort).create(any());
        order.verify(updatePromptLikeCountPort).increase(10L);
    }

    @DisplayName("좋아요를 삭제하고 감소된 좋아요 수를 반환한다")
    @Test
    void unlike() {
        when(updatePromptLikeCountPort.decrease(10L)).thenReturn(2L);

        LikeInfo info = service.unlike(new LikePromptCommand(1L, 10L));

        assertThat(info).isEqualTo(new LikeInfo(10L, false, 2L));
        InOrder order = inOrder(deletePostInteractionPort, updatePromptLikeCountPort);
        order.verify(deletePostInteractionPort).delete(1L, 10L, InteractionType.LIKE);
        order.verify(updatePromptLikeCountPort).decrease(10L);
    }
}
