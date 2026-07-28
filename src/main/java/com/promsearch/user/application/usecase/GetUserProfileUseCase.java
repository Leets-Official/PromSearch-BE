package com.promsearch.user.application.usecase;

import com.promsearch.user.application.usecase.dto.UserInfo;

public interface GetUserProfileUseCase {

    UserInfo getMyProfile(Long userId);
}
