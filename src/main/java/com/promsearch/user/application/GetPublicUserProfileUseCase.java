package com.promsearch.user.application;

public interface GetPublicUserProfileUseCase {

    PublicUserProfileInfo getProfile(Long userId);
}
