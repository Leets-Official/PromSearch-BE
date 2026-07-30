package com.promsearch.user.application.port.out.user;

import java.util.List;

public interface SaveUserInterestTagPort {

    void save(Long userId, List<String> jobTags, List<String> taskTags);
}
