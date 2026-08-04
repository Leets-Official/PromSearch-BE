package com.promsearch.user.application.port.out.user;

public interface UserProfileStatsReader {

    UserProfileStats getByUserId(Long userId);
}
