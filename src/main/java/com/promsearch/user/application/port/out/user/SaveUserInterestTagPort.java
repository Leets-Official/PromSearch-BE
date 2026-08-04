package com.promsearch.user.application.port.out.user;

import java.util.List;

public interface SaveUserInterestTagPort {

    void save(Long userId, List<Long> tagIds);

    void replace(Long userId, List<Long> tagIds);
}
