package com.promsearch.community.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.promsearch.community.application.port.out.comment.LoadCommentAuthorPort;
import com.promsearch.community.application.port.out.comment.LoadCommentAuthorPort.CommentAuthorSnapshot;
import com.promsearch.community.application.port.out.comment.LoadCommentPort;
import com.promsearch.community.application.port.out.comment.LoadCommentTargetPort;
import com.promsearch.community.application.port.out.comment.LoadCommentTargetPort.CommentTargetSnapshot;
import com.promsearch.community.application.usecase.dto.CommentListInfo;
import com.promsearch.community.application.usecase.dto.GetCommentsQuery;
import com.promsearch.community.domain.Comment;
import com.promsearch.community.domain.Comment.CommentId;
import com.promsearch.community.domain.enums.CommentStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentQueryServiceTest {

    @Mock
    private LoadCommentPort loadCommentPort;

    @Mock
    private LoadCommentAuthorPort loadCommentAuthorPort;

    @Mock
    private LoadCommentTargetPort loadCommentTargetPort;

    private CommentQueryService commentQueryService;

    @BeforeEach
    void setUp() {
        commentQueryService = new CommentQueryService(
                loadCommentPort,
                loadCommentAuthorPort,
                loadCommentTargetPort
        );
    }

    @DisplayName("댓글과 답글을 최신순으로 묶고 작성자 여부를 계산한다")
    @Test
    void getCommentsGroupsRepliesAndCalculatesViewerFlags() {
        Comment activeParent = comment(20L, 1L, 2L, null, "부모 댓글", CommentStatus.ACTIVE, 5);
        Comment activeReply = comment(21L, 1L, 1L, 20L, "답글", CommentStatus.ACTIVE, 4);
        Comment deletedParent = comment(10L, 1L, 3L, null, "삭제 전 내용", CommentStatus.DELETED, 3);
        Comment replyOfDeletedParent = comment(11L, 1L, 2L, 10L, "남아 있는 답글", CommentStatus.ACTIVE, 2);
        Comment deletedLeaf = comment(5L, 1L, 2L, null, "답글 없는 삭제 댓글", CommentStatus.DELETED, 1);

        given(loadCommentTargetPort.getActivePublicById(1L))
                .willReturn(new CommentTargetSnapshot(1L, 1L));
        given(loadCommentPort.listByPostId(1L))
                .willReturn(List.of(activeParent, activeReply, deletedParent, replyOfDeletedParent, deletedLeaf));
        given(loadCommentAuthorPort.batchGetByIds(Set.of(1L, 2L)))
                .willReturn(Map.of(
                        1L, new CommentAuthorSnapshot(1L, "prompt-owner", null),
                        2L, new CommentAuthorSnapshot(2L, "viewer", "viewer.png")
                ));

        CommentListInfo result = commentQueryService.getComments(GetCommentsQuery.of(1L, 2L));

        assertThat(result.comments()).hasSize(2);
        assertThat(result.comments().get(0).commentId()).isEqualTo(20L);
        assertThat(result.comments().get(0).mine()).isTrue();
        assertThat(result.comments().get(0).replies()).singleElement()
                .satisfies(reply -> {
                    assertThat(reply.commentId()).isEqualTo(21L);
                    assertThat(reply.promptAuthor()).isTrue();
                    assertThat(reply.mine()).isFalse();
                });
        assertThat(result.comments().get(1).status()).isEqualTo(CommentStatus.DELETED);
        assertThat(result.comments().get(1).content()).isEqualTo("삭제된 댓글입니다.");
        assertThat(result.comments().get(1).author()).isNull();
        assertThat(result.comments().get(1).mine()).isFalse();
        assertThat(result.comments().get(1).promptAuthor()).isFalse();
        assertThat(result.comments().get(1).replies()).singleElement()
                .satisfies(reply -> assertThat(reply.commentId()).isEqualTo(11L));
    }

    @DisplayName("비로그인 조회에서는 모든 mine 값을 false로 반환한다")
    @Test
    void getCommentsForAnonymousViewer() {
        Comment parent = comment(1L, 1L, 2L, null, "댓글", CommentStatus.ACTIVE, 1);
        given(loadCommentTargetPort.getActivePublicById(1L))
                .willReturn(new CommentTargetSnapshot(1L, 1L));
        given(loadCommentPort.listByPostId(1L)).willReturn(List.of(parent));
        given(loadCommentAuthorPort.batchGetByIds(Set.of(2L)))
                .willReturn(Map.of(2L, new CommentAuthorSnapshot(2L, "author", null)));

        CommentListInfo result = commentQueryService.getComments(GetCommentsQuery.of(1L, null));

        assertThat(result.comments()).singleElement()
                .satisfies(comment -> assertThat(comment.mine()).isFalse());
    }

    private Comment comment(
            Long id,
            Long postId,
            Long userId,
            Long parentId,
            String content,
            CommentStatus status,
            long seconds
    ) {
        Instant createdAt = Instant.parse("2026-07-28T00:00:00Z").plusSeconds(seconds);
        return Comment.reconstruct(
                new CommentId(id),
                postId,
                userId,
                content,
                status,
                parentId,
                createdAt,
                createdAt,
                status == CommentStatus.DELETED ? createdAt : null
        );
    }
}
