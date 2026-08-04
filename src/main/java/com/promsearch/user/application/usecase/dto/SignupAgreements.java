package com.promsearch.user.application.usecase.dto;

import com.promsearch.user.domain.UserAgreement;
import com.promsearch.user.domain.enums.AgreementType;
import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import java.time.Instant;
import java.util.List;

public record SignupAgreements(
        boolean serviceTerms,
        boolean communityTerms,
        boolean contentPolicy,
        boolean age14OrOver,
        boolean marketing
) {
    public SignupAgreements {
        if (!serviceTerms || !communityTerms || !contentPolicy || !age14OrOver) {
            throw new UserDomainException(UserErrorCode.REQUIRED_AGREEMENT_NOT_ACCEPTED);
        }
    }

    public static SignupAgreements of(
            Boolean serviceTerms,
            Boolean communityTerms,
            Boolean contentPolicy,
            Boolean age14OrOver,
            Boolean marketing
    ) {
        if (serviceTerms == null || communityTerms == null || contentPolicy == null
                || age14OrOver == null || marketing == null) {
            throw new UserDomainException(UserErrorCode.INVALID_AGREEMENT);
        }
        return new SignupAgreements(serviceTerms, communityTerms, contentPolicy, age14OrOver, marketing);
    }

    public static SignupAgreements requiredAndNoMarketing() {
        return new SignupAgreements(true, true, true, true, false);
    }

    public List<UserAgreement> toUserAgreements(Instant agreedAt) {
        return List.of(
                UserAgreement.create(AgreementType.SERVICE_TERMS, serviceTerms, agreedAt),
                UserAgreement.create(AgreementType.COMMUNITY_TERMS, communityTerms, agreedAt),
                UserAgreement.create(AgreementType.CONTENT_POLICY, contentPolicy, agreedAt),
                UserAgreement.create(AgreementType.AGE_14_OR_OVER, age14OrOver, agreedAt),
                UserAgreement.create(AgreementType.MARKETING, marketing, agreedAt)
        );
    }
}
