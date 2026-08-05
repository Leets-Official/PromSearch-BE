package com.promsearch.user.application.port.out.tag;

import java.util.List;

public interface LoadUserInterestTagsPort {

    List<InterestTagRow> loadByUserId(Long userId);
}
