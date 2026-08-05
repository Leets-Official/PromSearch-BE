package com.promsearch.community.application.service.query;

import com.promsearch.community.application.port.out.comment.LoadCommentAuthorPort;
import com.promsearch.community.application.port.out.comment.LoadCommentAuthorPort.CommentAuthorSnapshot;
import com.promsearch.community.application.port.out.comment.LoadCommentPort;
import com.promsearch.community.application.port.out.comment.LoadCommentPort.CommentPage;
import com.promsearch.community.application.port.out.comment.LoadCommentPort.ReplyPage;
import com.promsearch.community.application.port.out.comment.LoadCommentTargetPort;
import com.promsearch.community.application.port.out.comment.LoadCommentTargetPort.CommentTargetSnapshot;
import com.promsearch.community.application.usecase.GetCommentRepliesUseCase;
import com.promsearch.community.application.usecase.GetCommentsUseCase;
import com.promsearch.community.application.usecase.dto.CommentAuthorInfo;
import com.promsearch.community.application.usecase.dto.CommentInfo;
import com.promsearch.community.application.usecase.dto.CommentListInfo;
import com.promsearch.community.application.usecase.dto.CommentReplyInfo;
import com.promsearch.community.application.usecase.dto.CommentReplyListInfo;
import com.promsearch.community.application.usecase.dto.GetCommentRepliesQuery;
import com.promsearch.community.application.usecase.dto.GetCommentsQuery;
import com.promsearch.community.domain.Comment;
import com.promsearch.community.domain.exception.CommunityDomainException;
import com.promsearch.community.domain.exception.CommunityErrorCode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentQueryService implements GetCommentsUseCase, GetCommentRepliesUseCase {

    private static final String DELETED_COMMENT_CONTENT = "삭제된 댓글입니다.";
    private static final String HIDDEN_COMMENT_CONTENT = "블라인드 처리된 댓글입니다.";

    private final LoadCommentPort loadCommentPort;
    private final LoadCommentAuthorPort loadCommentAuthorPort;
    private final LoadCommentTargetPort loadCommentTargetPort;

    @Override
    public CommentListInfo getComments(GetCommentsQuery query) {
        CommentTargetSnapshot target =
                loadCommentTargetPort.getActivePublicById(query.postId());
        CommentPage page =
                loadCommentPort.listParentPage(query.postId(), query.cursor(), query.size());
        Set<Long> authorIds = page.comments().stream()
                .filter(Comment::isActive)
                .map(Comment::getUserId)
                .collect(Collectors.toSet());
        Map<Long, CommentAuthorSnapshot> authors =
                loadCommentAuthorPort.batchGetByIds(authorIds);
        List<CommentInfo> comments = page.comments().stream()
                .map(comment -> toCommentInfo(
                        comment,
                        authors,
                        target,
                        query.viewerId(),
                        page.replyCounts().getOrDefault(comment.getCommentId().id(), 0L)
                ))
                .toList();

        return new CommentListInfo(comments, page.nextCursor(), page.hasNext());
    }

    @Override
    public CommentReplyListInfo getReplies(GetCommentRepliesQuery query) {
        Comment parent = loadCommentPort.getById(query.parentCommentId());
        if (parent.getParentCommentId() != null) {
            throw new CommunityDomainException(CommunityErrorCode.INVALID_PARENT_COMMENT);
        }
        CommentTargetSnapshot target =
                loadCommentTargetPort.getActivePublicById(parent.getPostId());
        ReplyPage page = loadCommentPort.listReplyPage(
                query.parentCommentId(),
                query.cursor(),
                query.size()
        );
        Set<Long> authorIds = page.replies().stream()
                .map(Comment::getUserId)
                .collect(Collectors.toSet());
        Map<Long, CommentAuthorSnapshot> authors =
                loadCommentAuthorPort.batchGetByIds(authorIds);
        List<CommentReplyInfo> replies = page.replies().stream()
                .map(reply -> toReplyInfo(reply, authors, target, query.viewerId()))
                .toList();

        return new CommentReplyListInfo(replies, page.nextCursor(), page.hasNext());
    }

    private CommentInfo toCommentInfo(
            Comment comment,
            Map<Long, CommentAuthorSnapshot> authors,
            CommentTargetSnapshot target,
            Long viewerId,
            long replyCount
    ) {
        boolean masked = comment.isDeleted() || comment.isHidden();
        String content = comment.isDeleted() ? DELETED_COMMENT_CONTENT
                : comment.isHidden() ? HIDDEN_COMMENT_CONTENT
                : comment.getContent();
        return new CommentInfo(
                comment.getCommentId().id(),
                comment.getParentCommentId(),
                masked ? null : toAuthorInfo(getAuthor(authors, comment.getUserId())),
                content,
                comment.getStatus(),
                !masked && isMine(comment, viewerId),
                !masked && target.authorId().equals(comment.getUserId()),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                replyCount
        );
    }

    private CommentReplyInfo toReplyInfo(
            Comment comment,
            Map<Long, CommentAuthorSnapshot> authors,
            CommentTargetSnapshot target,
            Long viewerId
    ) {
        boolean masked = comment.isHidden();
        return new CommentReplyInfo(
                comment.getCommentId().id(),
                comment.getParentCommentId(),
                masked ? null : toAuthorInfo(getAuthor(authors, comment.getUserId())),
                masked ? HIDDEN_COMMENT_CONTENT : comment.getContent(),
                comment.getStatus(),
                !masked && isMine(comment, viewerId),
                !masked && target.authorId().equals(comment.getUserId()),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }

    private CommentAuthorSnapshot getAuthor(
            Map<Long, CommentAuthorSnapshot> authors,
            Long userId
    ) {
        CommentAuthorSnapshot author = authors.get(userId);
        if (author == null) {
            throw new CommunityDomainException(CommunityErrorCode.COMMENT_AUTHOR_NOT_FOUND);
        }
        return author;
    }

    private CommentAuthorInfo toAuthorInfo(CommentAuthorSnapshot author) {
        return new CommentAuthorInfo(
                author.userId(),
                author.nickname(),
                author.profileImageUrl()
        );
    }

    private boolean isMine(Comment comment, Long viewerId) {
        return viewerId != null && viewerId.equals(comment.getUserId());
    }
}
