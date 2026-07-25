package com.promsearch.user.application.usecase;

import com.promsearch.user.application.usecase.dto.UpdateUserProfileCommand;
import com.promsearch.user.application.usecase.dto.UserInfo;

public interface UpdateUserProfileUseCase {

    UserInfo updateProfile(UpdateUserProfileCommand command);
}
