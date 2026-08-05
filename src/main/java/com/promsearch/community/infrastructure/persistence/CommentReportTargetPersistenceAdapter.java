package com.promsearch.community.infrastructure.persistence;

import com.promsearch.moderation.application.port.out.target.LoadCommentReportTargetPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentReportTargetPersistenceAdapter implements LoadCommentReportTargetPort {

    private final CommentRepository commentRepository;

    @Override
    public boolean exists(Long commentId) {
        return commentRepository.existsReportableById(commentId);
    }
}
