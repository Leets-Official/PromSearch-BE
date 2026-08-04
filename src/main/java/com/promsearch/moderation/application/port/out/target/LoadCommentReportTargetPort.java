package com.promsearch.moderation.application.port.out.target;

public interface LoadCommentReportTargetPort {

    boolean exists(Long commentId);
}
