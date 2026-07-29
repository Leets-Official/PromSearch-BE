package com.promsearch.community.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.promsearch.community.application.service.command.CommentCommandService;
import com.promsearch.community.application.service.query.CommentQueryService;
import com.promsearch.community.application.usecase.dto.CommentInfo;
import com.promsearch.community.application.usecase.dto.CommentListInfo;
import com.promsearch.community.application.usecase.dto.CommentReplyInfo;
import com.promsearch.community.application.usecase.dto.CreateCommentCommand;
import com.promsearch.community.application.usecase.dto.CreateCommentReplyCommand;
import com.promsearch.community.application.usecase.dto.DeleteCommentCommand;
import com.promsearch.community.application.usecase.dto.GetCommentsQuery;
import com.promsearch.community.application.usecase.dto.UpdateCommentCommand;
import com.promsearch.community.domain.enums.CommentStatus;
import com.promsearch.community.domain.exception.CommunityDomainException;
import com.promsearch.community.domain.exception.CommunityErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CommentCrudIntegrationTest {

    @Autowired
    private CommentCommandService commentCommandService;

    @Autowired
    private CommentQueryService commentQueryService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("""
                insert into users (
                    user_id, email, password, nickname, name, points, role, grade, status, created_at, updated_at
                ) values
                    (1, 'owner@test.com', 'password', 'owner', 'Owner', 0, 'USER', 'NORMAL', 'ACTIVE',
                     current_timestamp, current_timestamp),
                    (2, 'commenter@test.com', 'password', 'commenter', 'Commenter', 0, 'USER', 'NORMAL', 'ACTIVE',
                     current_timestamp, current_timestamp)
                """);
        jdbcTemplate.update("""
                insert into posts (
                    post_id, user_id, title, status, visibility, price_point, created_at, updated_at
                ) values (
                    10, 1, '댓글 테스트 프롬프트', 'ACTIVE', 'PUBLIC', 0,
                    current_timestamp, current_timestamp
                ), (
                    11, 1, '비공개 프롬프트', 'ACTIVE', 'PRIVATE', 0,
                    current_timestamp, current_timestamp
                )
                """);
        jdbcTemplate.update("""
                insert into post_statistics (
                    post_id, view_count, copy_count, like_count, report_count, comment_count, created_at, updated_at
                ) values (
                    10, 0, 0, 0, 0, 0, current_timestamp, current_timestamp
                )
                """);
    }

    @DisplayName("댓글 CRUD와 답글, 댓글 수를 실제 저장소에서 일관되게 처리한다")
    @Test
    void commentCrudFlow() {
        CommentInfo parent = commentCommandService.createComment(
                CreateCommentCommand.of(10L, 2L, "첫 댓글")
        );
        CommentReplyInfo reply = commentCommandService.createReply(
                CreateCommentReplyCommand.of(parent.commentId(), 1L, "작성자 답글")
        );

        CommentInfo updated = commentCommandService.updateComment(
                UpdateCommentCommand.of(parent.commentId(), 2L, "수정된 댓글")
        );
        assertThat(updated.content()).isEqualTo("수정된 댓글");

        CommentListInfo beforeDelete = commentQueryService.getComments(GetCommentsQuery.of(10L, 2L));
        assertThat(beforeDelete.comments()).singleElement()
                .satisfies(comment -> {
                    assertThat(comment.mine()).isTrue();
                    assertThat(comment.replies()).singleElement()
                            .satisfies(savedReply -> {
                                assertThat(savedReply.commentId()).isEqualTo(reply.commentId());
                                assertThat(savedReply.promptAuthor()).isTrue();
                            });
                });
        assertThat(commentCount()).isEqualTo(2L);

        commentCommandService.deleteComment(DeleteCommentCommand.of(parent.commentId(), 2L));

        CommentListInfo afterParentDelete = commentQueryService.getComments(GetCommentsQuery.of(10L, null));
        assertThat(afterParentDelete.comments()).singleElement()
                .satisfies(comment -> {
                    assertThat(comment.status()).isEqualTo(CommentStatus.DELETED);
                    assertThat(comment.content()).isEqualTo("삭제된 댓글입니다.");
                    assertThat(comment.author()).isNull();
                    assertThat(comment.mine()).isFalse();
                    assertThat(comment.promptAuthor()).isFalse();
                    assertThat(comment.replies()).hasSize(1);
                });
        assertThat(commentCount()).isEqualTo(1L);

        commentCommandService.deleteComment(DeleteCommentCommand.of(reply.commentId(), 1L));

        assertThat(commentQueryService.getComments(GetCommentsQuery.of(10L, null)).comments()).isEmpty();
        assertThat(commentCount()).isZero();
    }

    @DisplayName("비공개 프롬프트의 댓글 목록은 조회할 수 없다")
    @Test
    void privatePromptCommentsAreNotPublic() {
        assertThatThrownBy(() -> commentQueryService.getComments(GetCommentsQuery.of(11L, null)))
                .isInstanceOf(CommunityDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CommunityErrorCode.COMMENT_TARGET_PROMPT_NOT_FOUND);
    }

    private Long commentCount() {
        return jdbcTemplate.queryForObject(
                "select comment_count from post_statistics where post_id = 10",
                Long.class
        );
    }
}
