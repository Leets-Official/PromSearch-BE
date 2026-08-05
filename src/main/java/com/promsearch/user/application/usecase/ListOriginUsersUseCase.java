package com.promsearch.user.application.usecase;

import com.promsearch.user.application.usecase.dto.OriginUserListInfo;
import com.promsearch.user.application.usecase.dto.OriginUserListQuery;

public interface ListOriginUsersUseCase {

    OriginUserListInfo list(OriginUserListQuery query);
}
