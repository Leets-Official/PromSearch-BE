package com.promsearch.user.application.port.out;

public interface UserProfileStatsReader {

    UserProfileStats getByUserId(Long userId);
}
