package com.promsearch.user.domain;

import com.promsearch.user.domain.enums.AgreementType;
import java.time.Instant;

public record UserAgreement(AgreementType type, String version, boolean agreed, Instant agreedAt) {
    public static UserAgreement create(AgreementType type, boolean agreed, Instant agreedAt) {
        return new UserAgreement(type, type.getVersion(), agreed, agreedAt);
    }
}
