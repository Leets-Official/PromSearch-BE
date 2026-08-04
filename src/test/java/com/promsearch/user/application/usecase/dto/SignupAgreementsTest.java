package com.promsearch.user.application.usecase.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.promsearch.user.domain.enums.AgreementType;
import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SignupAgreementsTest {

    @Test
    void allowsSignupWhenMarketingIsNotAccepted() {
        SignupAgreements agreements = SignupAgreements.of(true, true, true, true, false);

        assertThat(agreements.toUserAgreements(Instant.parse("2026-08-05T00:00:00Z")))
                .extracting(agreement -> agreement.type())
                .containsExactly(
                        AgreementType.SERVICE_TERMS,
                        AgreementType.COMMUNITY_TERMS,
                        AgreementType.CONTENT_POLICY,
                        AgreementType.AGE_14_OR_OVER,
                        AgreementType.MARKETING
                );
        assertThat(agreements.toUserAgreements(Instant.parse("2026-08-05T00:00:00Z"))
                .getLast().agreed()).isFalse();
    }

    @Test
    void rejectsSignupWhenARequiredAgreementIsNotAccepted() {
        assertThatThrownBy(() -> SignupAgreements.of(true, false, true, true, false))
                .isInstanceOf(UserDomainException.class)
                .extracting("baseCode")
                .isEqualTo(UserErrorCode.REQUIRED_AGREEMENT_NOT_ACCEPTED);
    }
}
