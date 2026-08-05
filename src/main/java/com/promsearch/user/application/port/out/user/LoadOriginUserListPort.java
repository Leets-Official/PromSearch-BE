package com.promsearch.user.application.port.out.user;

import com.promsearch.user.application.usecase.dto.OriginUserListInfo;
import com.promsearch.user.application.usecase.dto.OriginUserListQuery;

public interface LoadOriginUserListPort {

    OriginUserListInfo list(OriginUserListQuery query);
}
