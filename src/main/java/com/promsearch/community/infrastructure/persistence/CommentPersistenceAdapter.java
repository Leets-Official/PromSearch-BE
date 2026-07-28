package com.promsearch.community.infrastructure.persistence;

import com.promsearch.community.application.port.out.comment.LoadCommentPort;
import com.promsearch.community.application.port.out.comment.SaveCommentPort;
import com.promsearch.community.domain.Comment;
import com.promsearch.community.domain.Comment.CommentId;
import com.promsearch.community.domain.exception.CommunityDomainException;
import com.promsearch.community.domain.exception.CommunityErrorCode;
import com.promsearch.community.infrastructure.persistence.entity.CommentJpaEntity;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentPersistenceAdapter implements LoadCommentPort, SaveCommentPort {

    private final CommentRepository commentRepository;

    @Override
    public Comment getByIdForUpdate(Long commentId) {
        new CommentId(commentId);
        return commentRepository.findByIdForUpdate(commentId)
                .map(CommentJpaEntity::toDomain)
                .orElseThrow(() -> new CommunityDomainException(CommunityErrorCode.COMMENT_NOT_FOUND));
    }

    @Override
    public List<Comment> listByPostId(Long postId) {
        if (postId == null || postId <= 0) {
            throw new CommunityDomainException(CommunityErrorCode.INVALID_COMMENT_POST_ID);
        }
        return commentRepository.findAllByPostIdOrderByCreatedAtDesc(postId).stream()
                .map(CommentJpaEntity::toDomain)
                .toList();
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
}
