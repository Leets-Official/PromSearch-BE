package com.promsearch.user.infrastructure.persistence;

import com.promsearch.user.application.port.out.user.SaveUserAgreementPort;
import com.promsearch.user.domain.UserAgreement;
import com.promsearch.user.infrastructure.persistence.entity.UserAgreementJpaEntity;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserAgreementPersistenceAdapter implements SaveUserAgreementPort {

    private final UserAgreementRepository userAgreementRepository;

    @Override
    public void saveAll(Long userId, List<UserAgreement> agreements) {
        userAgreementRepository.saveAll(agreements.stream()
                .map(agreement -> UserAgreementJpaEntity.create(userId, agreement))
                .toList());
    }
}
