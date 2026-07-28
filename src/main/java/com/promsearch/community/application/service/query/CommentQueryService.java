package com.promsearch.community.application.service.query;

import com.promsearch.community.application.port.out.comment.LoadCommentAuthorPort;
import com.promsearch.community.application.port.out.comment.LoadCommentAuthorPort.CommentAuthorSnapshot;
import com.promsearch.community.application.port.out.comment.LoadCommentPort;
import com.promsearch.community.application.port.out.comment.LoadCommentTargetPort;
import com.promsearch.community.application.port.out.comment.LoadCommentTargetPort.CommentTargetSnapshot;
import com.promsearch.community.application.usecase.GetCommentsUseCase;
import com.promsearch.community.application.usecase.dto.CommentAuthorInfo;
import com.promsearch.community.application.usecase.dto.CommentInfo;
import com.promsearch.community.application.usecase.dto.CommentListInfo;
import com.promsearch.community.application.usecase.dto.CommentReplyInfo;
import com.promsearch.community.application.usecase.dto.GetCommentsQuery;
import com.promsearch.community.domain.Comment;
import com.promsearch.community.domain.exception.CommunityDomainException;
import com.promsearch.community.domain.exception.CommunityErrorCode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentQueryService implements GetCommentsUseCase {

    private static final String DELETED_COMMENT_CONTENT = "삭제된 댓글입니다.";

    private final LoadCommentPort loadCommentPort;
    private final LoadCommentAuthorPort loadCommentAuthorPort;
    private final LoadCommentTargetPort loadCommentTargetPort;

    @Override
    public CommentListInfo getComments(GetCommentsQuery query) {
        CommentTargetSnapshot target = loadCommentTargetPort.getActiveById(query.postId());
        List<Comment> allComments = loadCommentPort.listByPostId(query.postId());

        Map<Long, List<Comment>> activeRepliesByParentId = groupActiveReplies(allComments);
        List<Comment> visibleParents = allComments.stream()
                .filter(comment -> comment.getParentCommentId() == null)
                .filter(comment -> comment.isActive()
                        || comment.isDeleted()
                        && activeRepliesByParentId.containsKey(comment.getCommentId().id()))
                .toList();

        Set<Long> authorIds = collectAuthorIds(visibleParents, activeRepliesByParentId);
        Map<Long, CommentAuthorSnapshot> authors = loadCommentAuthorPort.batchGetByIds(authorIds);

        List<CommentInfo> comments = visibleParents.stream()
                .map(parent -> toCommentInfo(
                        parent,
                        activeRepliesByParentId.getOrDefault(parent.getCommentId().id(), List.of()),
                        authors,
                        target,
                        query.viewerId()
                ))
                .toList();

        return new CommentListInfo(comments);
    }

    private Map<Long, List<Comment>> groupActiveReplies(List<Comment> comments) {
        Map<Long, List<Comment>> repliesByParentId = new HashMap<>();
        for (Comment comment : comments) {
            if (comment.getParentCommentId() != null && comment.isActive()) {
                repliesByParentId
                        .computeIfAbsent(comment.getParentCommentId(), ignored -> new ArrayList<>())
                        .add(comment);
            }
        }
        return repliesByParentId;
    }

    private Set<Long> collectAuthorIds(
            List<Comment> parents,
            Map<Long, List<Comment>> repliesByParentId
    ) {
        Set<Long> authorIds = new HashSet<>();
        for (Comment parent : parents) {
            if (parent.isActive()) {
                authorIds.add(parent.getUserId());
            }
            repliesByParentId.getOrDefault(parent.getCommentId().id(), List.of())
                    .forEach(reply -> authorIds.add(reply.getUserId()));
        }
        return authorIds;
    }

    private CommentInfo toCommentInfo(
            Comment comment,
            List<Comment> replies,
            Map<Long, CommentAuthorSnapshot> authors,
            CommentTargetSnapshot target,
            Long viewerId
    ) {
        List<CommentReplyInfo> replyInfos = replies.stream()
                .map(reply -> toReplyInfo(reply, authors, target, viewerId))
                .toList();
        boolean deleted = comment.isDeleted();

        return new CommentInfo(
                comment.getCommentId().id(),
                comment.getParentCommentId(),
                deleted ? null : toAuthorInfo(getAuthor(authors, comment.getUserId())),
                deleted ? DELETED_COMMENT_CONTENT : comment.getContent(),
                comment.getStatus(),
                !deleted && isMine(comment, viewerId),
                !deleted && target.authorId().equals(comment.getUserId()),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                replyInfos
        );
    }

    private CommentReplyInfo toReplyInfo(
            Comment comment,
            Map<Long, CommentAuthorSnapshot> authors,
            CommentTargetSnapshot target,
            Long viewerId
    ) {
        return new CommentReplyInfo(
                comment.getCommentId().id(),
                comment.getParentCommentId(),
                toAuthorInfo(getAuthor(authors, comment.getUserId())),
                comment.getContent(),
                comment.getStatus(),
                isMine(comment, viewerId),
                target.authorId().equals(comment.getUserId()),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }

    private CommentAuthorSnapshot getAuthor(Map<Long, CommentAuthorSnapshot> authors, Long userId) {
        CommentAuthorSnapshot author = authors.get(userId);
        if (author == null) {
            throw new CommunityDomainException(CommunityErrorCode.COMMENT_AUTHOR_NOT_FOUND);
        }
        return author;
    }

    private CommentAuthorInfo toAuthorInfo(CommentAuthorSnapshot author) {
        return new CommentAuthorInfo(author.userId(), author.nickname(), author.profileImageUrl());
    }

    private boolean isMine(Comment comment, Long viewerId) {
        return viewerId != null && viewerId.equals(comment.getUserId());
    }
}
