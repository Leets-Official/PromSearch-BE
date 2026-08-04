package com.promsearch.commerce.application.port.out.copy;

public interface CheckPostCopyPort {

    boolean isCopied(Long userId, Long postId);
}
