package com.promsearch.user.application.port.out.user;

import com.promsearch.user.application.usecase.dto.InterestTagInfo;
import java.util.List;

public interface LoadUserInterestTagPort {

    List<InterestTagInfo> listByUserId(Long userId);
}
