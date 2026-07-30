package com.promsearch.community.infrastructure.persistence;

import com.promsearch.community.application.port.out.comment.LoadCommentPort;
import com.promsearch.community.application.port.out.comment.LoadCommentPort.CommentPage;
import com.promsearch.community.application.port.out.comment.LoadCommentPort.ReplyPage;
import com.promsearch.community.application.port.out.comment.SaveCommentPort;
import com.promsearch.community.domain.Comment;
import com.promsearch.community.domain.Comment.CommentId;
import com.promsearch.community.domain.enums.CommentStatus;
import com.promsearch.community.domain.exception.CommunityDomainException;
import com.promsearch.community.domain.exception.CommunityErrorCode;
import com.promsearch.community.infrastructure.persistence.entity.CommentJpaEntity;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentPersistenceAdapter implements LoadCommentPort, SaveCommentPort {

    private final CommentRepository commentRepository;

    @Override
    public Comment getById(Long commentId) {
        new CommentId(commentId);
        return commentRepository.findById(commentId)
                .map(CommentJpaEntity::toDomain)
                .orElseThrow(() -> new CommunityDomainException(CommunityErrorCode.COMMENT_NOT_FOUND));
    }

    @Override
    public Comment getByIdForUpdate(Long commentId) {
        new CommentId(commentId);
        return commentRepository.findByIdForUpdate(commentId)
                .map(CommentJpaEntity::toDomain)
                .orElseThrow(() -> new CommunityDomainException(CommunityErrorCode.COMMENT_NOT_FOUND));
    }

    @Override
    public CommentPage listParentPage(Long postId, Long cursor, int size) {
        if (postId == null || postId <= 0) {
            throw new CommunityDomainException(CommunityErrorCode.INVALID_COMMENT_POST_ID);
        }
        validatePageSize(size);

        CommentJpaEntity cursorEntity = cursor == null ? null
                : commentRepository.findByIdAndPostIdAndParentCommentIdIsNull(cursor, postId)
                        .orElseThrow(() -> new CommunityDomainException(
                                CommunityErrorCode.COMMENT_NOT_FOUND));
        List<CommentJpaEntity> fetched = commentRepository.findParentPage(
                postId,
                CommentStatus.ACTIVE,
                CommentStatus.DELETED,
                cursorEntity == null ? null : cursorEntity.getCreatedAt(),
                cursorEntity == null ? null : cursor,
                PageRequest.of(0, size + 1)
        );
        boolean hasNext = fetched.size() > size;
        List<CommentJpaEntity> pageEntities = hasNext
                ? fetched.subList(0, size)
                : fetched;
        List<Comment> comments = pageEntities.stream()
                .map(CommentJpaEntity::toDomain)
                .toList();
        List<Long> parentIds = comments.stream()
                .map(comment -> comment.getCommentId().id())
                .toList();
        Map<Long, Long> replyCounts = parentIds.isEmpty()
                ? Map.of()
                : commentRepository.countRepliesByParentIds(parentIds, CommentStatus.ACTIVE)
                        .stream()
                        .collect(Collectors.toMap(
                                CommentRepository.ParentReplyCountProjection::getParentId,
                                CommentRepository.ParentReplyCountProjection::getReplyCount
                        ));
        Long nextCursor = hasNext
                ? comments.getLast().getCommentId().id()
                : null;
        return new CommentPage(comments, replyCounts, nextCursor, hasNext);
    }

    @Override
    public ReplyPage listReplyPage(Long parentCommentId, Long cursor, int size) {
        new CommentId(parentCommentId);
        validatePageSize(size);

        CommentJpaEntity cursorEntity = cursor == null ? null
                : commentRepository.findByIdAndParentCommentIdAndStatus(
                                cursor,
                                parentCommentId,
                                CommentStatus.ACTIVE)
                        .orElseThrow(() -> new CommunityDomainException(
                                CommunityErrorCode.COMMENT_NOT_FOUND));
        List<CommentJpaEntity> fetched = commentRepository.findReplyPage(
                parentCommentId,
                CommentStatus.ACTIVE,
                cursorEntity == null ? null : cursorEntity.getCreatedAt(),
                cursorEntity == null ? null : cursor,
                PageRequest.of(0, size + 1)
        );
        boolean hasNext = fetched.size() > size;
        List<Comment> replies = (hasNext ? fetched.subList(0, size) : fetched).stream()
                .map(CommentJpaEntity::toDomain)
                .toList();
        Long nextCursor = hasNext
                ? replies.getLast().getCommentId().id()
                : null;
        return new ReplyPage(replies, nextCursor, hasNext);
    }

    @Override
    public Comment create(Comment comment) {
        CommentJpaEntity entity = CommentJpaEntity.create(
                comment.getPostId(),
                comment.getUserId(),
                comment.getContent(),
                comment.getParentCommentId()
        );
        return commentRepository.saveAndFlush(entity).toDomain();
    }

    @Override
    public Comment update(Comment comment) {
        Long commentId = comment.getCommentId().id();
        CommentJpaEntity entity = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommunityDomainException(CommunityErrorCode.COMMENT_NOT_FOUND));
        entity.updateFrom(comment);
        return commentRepository.saveAndFlush(entity).toDomain();
    }

    private void validatePageSize(int size) {
        if (size < 1 || size > 100) {
            throw new CommunityDomainException(CommunityErrorCode.INVALID_ID);
        }
    }
}
