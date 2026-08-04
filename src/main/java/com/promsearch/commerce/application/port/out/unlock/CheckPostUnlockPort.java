package com.promsearch.commerce.application.port.out.unlock;

public interface CheckPostUnlockPort {

    boolean isUnlocked(Long userId, Long postId);
}
