package com.promsearch.moderation.application.port.out.target;

public interface LoadPostReportTargetPort {

    boolean exists(Long postId);
}
