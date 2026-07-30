package com.promsearch.user.application.usecase;

import com.promsearch.user.application.usecase.dto.NicknameAvailabilityInfo;
import com.promsearch.user.application.usecase.dto.NicknameAvailabilityQuery;

public interface CheckNicknameAvailabilityUseCase {

    NicknameAvailabilityInfo checkAvailability(NicknameAvailabilityQuery query);
}
