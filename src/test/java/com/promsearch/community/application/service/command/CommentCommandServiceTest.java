package com.promsearch.community.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.promsearch.community.application.port.out.comment.AdjustCommentCountPort;
import com.promsearch.community.application.port.out.comment.LoadCommentAuthorPort;
import com.promsearch.community.application.port.out.comment.LoadCommentAuthorPort.CommentAuthorSnapshot;
import com.promsearch.community.application.port.out.comment.LoadCommentPort;
import com.promsearch.community.application.port.out.comment.LoadCommentTargetPort;
import com.promsearch.community.application.port.out.comment.LoadCommentTargetPort.CommentTargetSnapshot;
import com.promsearch.community.application.port.out.comment.SaveCommentPort;
import com.promsearch.community.application.usecase.dto.CommentInfo;
import com.promsearch.community.application.usecase.dto.CreateCommentCommand;
import com.promsearch.community.application.usecase.dto.CreateCommentReplyCommand;
import com.promsearch.community.application.usecase.dto.DeleteCommentCommand;
import com.promsearch.community.domain.Comment;
import com.promsearch.community.domain.Comment.CommentId;
import com.promsearch.community.domain.enums.CommentStatus;
import com.promsearch.community.domain.exception.CommunityDomainException;
import com.promsearch.community.domain.exception.CommunityErrorCode;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentCommandServiceTest {

    @Mock
    private LoadCommentPort loadCommentPort;
    @Mock
    private SaveCommentPort saveCommentPort;
    @Mock
    private LoadCommentAuthorPort loadCommentAuthorPort;
    @Mock
    private LoadCommentTargetPort loadCommentTargetPort;
    @Mock
    private AdjustCommentCountPort adjustCommentCountPort;

    private CommentCommandService commentCommandService;

    @BeforeEach
    void setUp() {
        commentCommandService = new CommentCommandService(
                loadCommentPort,
                saveCommentPort,
                loadCommentAuthorPort,
                loadCommentTargetPort,
                adjustCommentCountPort
        );
    }

    @DisplayName("댓글 생성과 댓글 수 증가는 하나의 유스케이스에서 처리한다")
    @Test
    void createCommentIncrementsCommentCount() {
        given(loadCommentTargetPort.getActiveById(10L))
                .willReturn(new CommentTargetSnapshot(10L, 1L));
        given(loadCommentAuthorPort.getActiveById(2L))
                .willReturn(new CommentAuthorSnapshot(2L, "author", null));
        given(saveCommentPort.create(any(Comment.class)))
                .willReturn(comment(100L, 10L, 2L, null, "댓글", CommentStatus.ACTIVE));

        CommentInfo result = commentCommandService.createComment(
                CreateCommentCommand.of(10L, 2L, "댓글")
        );

        assertThat(result.commentId()).isEqualTo(100L);
        assertThat(result.mine()).isTrue();
        assertThat(result.promptAuthor()).isFalse();
        verify(adjustCommentCountPort).increment(10L);
    }

    @DisplayName("답글에는 답글을 추가할 수 없다")
    @Test
    void createReplyRejectsReplyToReply() {
        given(loadCommentAuthorPort.getActiveById(2L))
                .willReturn(new CommentAuthorSnapshot(2L, "author", null));
        given(loadCommentPort.getByIdForUpdate(20L))
                .willReturn(comment(20L, 10L, 1L, 19L, "기존 답글", CommentStatus.ACTIVE));

        assertThatThrownBy(() -> commentCommandService.createReply(
                CreateCommentReplyCommand.of(20L, 2L, "중첩 답글")
        ))
                .isInstanceOf(CommunityDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CommunityErrorCode.REPLY_TO_REPLY_NOT_ALLOWED);

        verify(saveCommentPort, never()).create(any());
        verify(adjustCommentCountPort, never()).increment(any());
    }

    @DisplayName("다른 사용자는 댓글을 삭제할 수 없다")
    @Test
    void deleteCommentRejectsNonOwner() {
        given(loadCommentAuthorPort.getActiveById(2L))
                .willReturn(new CommentAuthorSnapshot(2L, "other", null));
        given(loadCommentPort.getByIdForUpdate(30L))
                .willReturn(comment(30L, 10L, 1L, null, "댓글", CommentStatus.ACTIVE));

        assertThatThrownBy(() -> commentCommandService.deleteComment(DeleteCommentCommand.of(30L, 2L)))
                .isInstanceOf(CommunityDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CommunityErrorCode.COMMENT_NOT_OWNED);

        verify(saveCommentPort, never()).update(any());
        verify(adjustCommentCountPort, never()).decrement(any());
    }

    private Comment comment(
            Long id,
            Long postId,
            Long userId,
            Long parentId,
            String content,
            CommentStatus status
    ) {
        Instant now = Instant.parse("2026-07-28T00:00:00Z");
        return Comment.reconstruct(
                new CommentId(id),
                postId,
                userId,
                content,
                status,
                parentId,
                now,
                now,
                status == CommentStatus.DELETED ? now : null
        );
    }
}
