package com.promsearch.user.application;

public interface UpdateUserProfileUseCase {

    UserInfo updateProfile(UpdateUserProfileCommand command);
}
