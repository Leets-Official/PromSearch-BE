package com.promsearch.community.infrastructure.persistence;

import com.promsearch.community.domain.enums.CommentStatus;
import com.promsearch.moderation.application.port.out.target.LoadCommentReportTargetPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentReportTargetPersistenceAdapter implements LoadCommentReportTargetPort {

    private final CommentRepository commentRepository;

    @Override
    public boolean exists(Long commentId) {
        return commentRepository.existsByIdAndStatus(commentId, CommentStatus.ACTIVE);
    }
}
