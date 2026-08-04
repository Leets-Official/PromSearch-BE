package com.promsearch.user.application.port.out.tag;

import com.promsearch.user.domain.enums.InterestTagType;
import java.util.List;

public interface ResolveInterestTagIdsPort {

    List<Long> resolve(InterestTagType type, List<Long> tagIds);
}
