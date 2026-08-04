package com.promsearch.user.application.usecase;

import com.promsearch.user.application.usecase.dto.PublicUserProfileInfo;

public interface GetPublicUserProfileUseCase {

    PublicUserProfileInfo getProfile(Long userId);
}
