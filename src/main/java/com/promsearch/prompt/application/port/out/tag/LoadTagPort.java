package com.promsearch.prompt.application.port.out.tag;

import com.promsearch.prompt.domain.Tag;
import java.util.Collection;
import java.util.List;

public interface LoadTagPort {

    List<Tag> batchGetByIds(Collection<Long> tagIds);
}
