package com.promsearch.community.application.service.command;

import com.promsearch.community.application.port.out.comment.AdjustCommentCountPort;
import com.promsearch.community.application.port.out.comment.LoadCommentAuthorPort;
import com.promsearch.community.application.port.out.comment.LoadCommentAuthorPort.CommentAuthorSnapshot;
import com.promsearch.community.application.port.out.comment.LoadCommentPort;
import com.promsearch.community.application.port.out.comment.LoadCommentTargetPort;
import com.promsearch.community.application.port.out.comment.LoadCommentTargetPort.CommentTargetSnapshot;
import com.promsearch.community.application.port.out.comment.SaveCommentPort;
import com.promsearch.community.application.usecase.CreateCommentReplyUseCase;
import com.promsearch.community.application.usecase.CreateCommentUseCase;
import com.promsearch.community.application.usecase.DeleteCommentUseCase;
import com.promsearch.community.application.usecase.UpdateCommentUseCase;
import com.promsearch.community.application.usecase.dto.CommentAuthorInfo;
import com.promsearch.community.application.usecase.dto.CommentInfo;
import com.promsearch.community.application.usecase.dto.CommentReplyInfo;
import com.promsearch.community.application.usecase.dto.CreateCommentCommand;
import com.promsearch.community.application.usecase.dto.CreateCommentReplyCommand;
import com.promsearch.community.application.usecase.dto.DeleteCommentCommand;
import com.promsearch.community.application.usecase.dto.UpdateCommentCommand;
import com.promsearch.community.domain.Comment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentCommandService implements
        CreateCommentUseCase,
        CreateCommentReplyUseCase,
        UpdateCommentUseCase,
        DeleteCommentUseCase {

    private final LoadCommentPort loadCommentPort;
    private final SaveCommentPort saveCommentPort;
    private final LoadCommentAuthorPort loadCommentAuthorPort;
    private final LoadCommentTargetPort loadCommentTargetPort;
    private final AdjustCommentCountPort adjustCommentCountPort;

    @Override
    public CommentInfo createComment(CreateCommentCommand command) {
        CommentTargetSnapshot target = loadCommentTargetPort.getActiveById(command.postId());
        CommentAuthorSnapshot author = loadCommentAuthorPort.getActiveById(command.userId());

        Comment savedComment = saveCommentPort.create(
                Comment.create(command.postId(), command.userId(), command.content(), null)
        );
        adjustCommentCountPort.increment(command.postId());

        return toCommentInfo(savedComment, author, target, command.userId());
    }

    @Override
    public CommentReplyInfo createReply(CreateCommentReplyCommand command) {
        CommentAuthorSnapshot author = loadCommentAuthorPort.getActiveById(command.userId());
        Comment parent = loadCommentPort.getByIdForUpdate(command.parentCommentId());
        parent.validateCanHaveReply();
        CommentTargetSnapshot target = loadCommentTargetPort.getActiveById(parent.getPostId());

        Comment savedReply = saveCommentPort.create(
                Comment.create(parent.getPostId(), command.userId(), command.content(), parent.getCommentId().id())
        );
        adjustCommentCountPort.increment(parent.getPostId());

        return toReplyInfo(savedReply, author, target, command.userId());
    }

    @Override
    public CommentInfo updateComment(UpdateCommentCommand command) {
        CommentAuthorSnapshot author = loadCommentAuthorPort.getActiveById(command.userId());
        Comment comment = loadCommentPort.getByIdForUpdate(command.commentId());
        CommentTargetSnapshot target = loadCommentTargetPort.getActiveById(comment.getPostId());

        comment.updateContent(command.userId(), command.content());
        Comment updatedComment = saveCommentPort.update(comment);

        return toCommentInfo(updatedComment, author, target, command.userId());
    }

    @Override
    public void deleteComment(DeleteCommentCommand command) {
        loadCommentAuthorPort.getActiveById(command.userId());
        Comment comment = loadCommentPort.getByIdForUpdate(command.commentId());

        comment.delete(command.userId());
        saveCommentPort.update(comment);
        adjustCommentCountPort.decrement(comment.getPostId());
    }

    private CommentInfo toCommentInfo(
            Comment comment,
            CommentAuthorSnapshot author,
            CommentTargetSnapshot target,
            Long viewerId
    ) {
        return new CommentInfo(
                comment.getCommentId().id(),
                comment.getParentCommentId(),
                toAuthorInfo(author),
                comment.getContent(),
                comment.getStatus(),
                viewerId.equals(comment.getUserId()),
                target.authorId().equals(comment.getUserId()),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                0L
        );
    }

    private CommentReplyInfo toReplyInfo(
            Comment comment,
            CommentAuthorSnapshot author,
            CommentTargetSnapshot target,
            Long viewerId
    ) {
        return new CommentReplyInfo(
                comment.getCommentId().id(),
                comment.getParentCommentId(),
                toAuthorInfo(author),
                comment.getContent(),
                comment.getStatus(),
                viewerId.equals(comment.getUserId()),
                target.authorId().equals(comment.getUserId()),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }

    private CommentAuthorInfo toAuthorInfo(CommentAuthorSnapshot author) {
        return new CommentAuthorInfo(author.userId(), author.nickname(), author.profileImageUrl());
    }
}
