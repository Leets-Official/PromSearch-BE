package com.promsearch.community.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.promsearch.community.application.port.out.comment.LoadCommentAuthorPort;
import com.promsearch.community.application.port.out.comment.LoadCommentAuthorPort.CommentAuthorSnapshot;
import com.promsearch.community.application.port.out.comment.LoadCommentPort;
import com.promsearch.community.application.port.out.comment.LoadCommentPort.CommentPage;
import com.promsearch.community.application.port.out.comment.LoadCommentPort.ReplyPage;
import com.promsearch.community.application.port.out.comment.LoadCommentTargetPort;
import com.promsearch.community.application.port.out.comment.LoadCommentTargetPort.CommentTargetSnapshot;
import com.promsearch.community.application.usecase.dto.CommentListInfo;
import com.promsearch.community.application.usecase.dto.CommentReplyListInfo;
import com.promsearch.community.application.usecase.dto.GetCommentRepliesQuery;
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

    @DisplayName("최상위 댓글 페이지와 활성 대댓글 개수를 반환한다")
    @Test
    void getParentCommentPage() {
        Comment activeParent =
                comment(20L, 1L, 2L, null, "부모 댓글", CommentStatus.ACTIVE, 5);
        Comment deletedParent =
                comment(10L, 1L, 3L, null, "삭제 전 내용", CommentStatus.DELETED, 3);
        given(loadCommentTargetPort.getActivePublicById(1L))
                .willReturn(new CommentTargetSnapshot(1L, 1L));
        given(loadCommentPort.listParentPage(1L, null, 2))
                .willReturn(new CommentPage(
                        List.of(activeParent, deletedParent),
                        Map.of(20L, 2L, 10L, 1L),
                        10L,
                        true
                ));
        given(loadCommentAuthorPort.batchGetByIds(Set.of(2L)))
                .willReturn(Map.of(
                        2L, new CommentAuthorSnapshot(2L, "viewer", "viewer.png")
                ));

        CommentListInfo result = commentQueryService.getComments(
                GetCommentsQuery.of(1L, 2L, null, 2));

        assertThat(result.comments()).hasSize(2);
        assertThat(result.nextCursor()).isEqualTo(10L);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.comments().get(0).mine()).isTrue();
        assertThat(result.comments().get(0).replyCount()).isEqualTo(2L);
        assertThat(result.comments().get(1).status()).isEqualTo(CommentStatus.DELETED);
        assertThat(result.comments().get(1).content()).isEqualTo("삭제된 댓글입니다.");
        assertThat(result.comments().get(1).author()).isNull();
        assertThat(result.comments().get(1).replyCount()).isEqualTo(1L);
    }

    @DisplayName("대댓글 페이지를 오래된 순서로 변환하고 사용자 여부를 계산한다")
    @Test
    void getReplyPage() {
        Comment parent =
                comment(20L, 1L, 2L, null, "부모 댓글", CommentStatus.ACTIVE, 1);
        Comment first =
                comment(21L, 1L, 1L, 20L, "첫 답글", CommentStatus.ACTIVE, 2);
        Comment second =
                comment(22L, 1L, 2L, 20L, "둘째 답글", CommentStatus.ACTIVE, 3);
        given(loadCommentPort.getById(20L)).willReturn(parent);
        given(loadCommentTargetPort.getActivePublicById(1L))
                .willReturn(new CommentTargetSnapshot(1L, 1L));
        given(loadCommentPort.listReplyPage(20L, null, 2))
                .willReturn(new ReplyPage(List.of(first, second), null, false));
        given(loadCommentAuthorPort.batchGetByIds(Set.of(1L, 2L)))
                .willReturn(Map.of(
                        1L, new CommentAuthorSnapshot(1L, "owner", null),
                        2L, new CommentAuthorSnapshot(2L, "viewer", null)
                ));

        CommentReplyListInfo result = commentQueryService.getReplies(
                GetCommentRepliesQuery.of(20L, 2L, null, 2));

        assertThat(result.replies())
                .extracting(reply -> reply.commentId())
                .containsExactly(21L, 22L);
        assertThat(result.replies().get(0).promptAuthor()).isTrue();
        assertThat(result.replies().get(1).mine()).isTrue();
        assertThat(result.hasNext()).isFalse();
    }

    @DisplayName("블라인드 처리된 부모 댓글은 마스킹되어 노출된다")
    @Test
    void getParentCommentPageMasksHiddenComment() {
        Comment hiddenParent =
                comment(30L, 1L, 4L, null, "블라인드 대상", CommentStatus.HIDDEN, 7);
        given(loadCommentTargetPort.getActivePublicById(1L))
                .willReturn(new CommentTargetSnapshot(1L, 1L));
        given(loadCommentPort.listParentPage(1L, null, 2))
                .willReturn(new CommentPage(List.of(hiddenParent), Map.of(), null, false));

        CommentListInfo result = commentQueryService.getComments(
                GetCommentsQuery.of(1L, 2L, null, 2));

        assertThat(result.comments()).singleElement()
                .satisfies(comment -> {
                    assertThat(comment.status()).isEqualTo(CommentStatus.HIDDEN);
                    assertThat(comment.content()).isEqualTo("블라인드 처리된 댓글입니다.");
                    assertThat(comment.author()).isNull();
                    assertThat(comment.mine()).isFalse();
                });
    }

    @DisplayName("블라인드 처리된 대댓글은 마스킹되어 노출된다")
    @Test
    void getReplyPageMasksHiddenReply() {
        Comment parent =
                comment(20L, 1L, 2L, null, "부모 댓글", CommentStatus.ACTIVE, 1);
        Comment hiddenReply =
                comment(23L, 1L, 4L, 20L, "블라인드 대상", CommentStatus.HIDDEN, 4);
        given(loadCommentPort.getById(20L)).willReturn(parent);
        given(loadCommentTargetPort.getActivePublicById(1L))
                .willReturn(new CommentTargetSnapshot(1L, 1L));
        given(loadCommentPort.listReplyPage(20L, null, 2))
                .willReturn(new ReplyPage(List.of(hiddenReply), null, false));
        given(loadCommentAuthorPort.batchGetByIds(Set.of(4L)))
                .willReturn(Map.of(4L, new CommentAuthorSnapshot(4L, "reported-user", null)));

        CommentReplyListInfo result = commentQueryService.getReplies(
                GetCommentRepliesQuery.of(20L, 2L, null, 2));

        assertThat(result.replies()).singleElement()
                .satisfies(reply -> {
                    assertThat(reply.status()).isEqualTo(CommentStatus.HIDDEN);
                    assertThat(reply.content()).isEqualTo("블라인드 처리된 댓글입니다.");
                    assertThat(reply.author()).isNull();
                    assertThat(reply.mine()).isFalse();
                });
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
