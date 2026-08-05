package com.promsearch.user.application.port.out.user;

import com.promsearch.user.domain.UserAgreement;
import java.util.List;

public interface SaveUserAgreementPort {
    void saveAll(Long userId, List<UserAgreement> agreements);
}
