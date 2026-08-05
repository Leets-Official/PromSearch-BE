package com.promsearch.user.infrastructure.persistence.entity;

import com.promsearch.common.BaseEntity;
import com.promsearch.user.domain.UserAgreement;
import com.promsearch.user.domain.enums.AgreementType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_agreements")
public class UserAgreementJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_agreement_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "agreement_type", nullable = false, length = 50)
    private AgreementType agreementType;

    @Column(name = "agreement_version", nullable = false, length = 50)
    private String agreementVersion;

    @Column(name = "agreed", nullable = false)
    private boolean agreed;

    @Column(name = "agreed_at", nullable = false)
    private Instant agreedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private UserAgreementJpaEntity(
            Long userId,
            AgreementType agreementType,
            String agreementVersion,
            boolean agreed,
            Instant agreedAt
    ) {
        this.userId = userId;
        this.agreementType = agreementType;
        this.agreementVersion = agreementVersion;
        this.agreed = agreed;
        this.agreedAt = agreedAt;
    }

    public static UserAgreementJpaEntity create(Long userId, UserAgreement agreement) {
        return UserAgreementJpaEntity.builder()
                .userId(userId)
                .agreementType(agreement.type())
                .agreementVersion(agreement.version())
                .agreed(agreement.agreed())
                .agreedAt(agreement.agreedAt())
                .build();
    }
}
