package com.promsearch.user.infrastructure.persistence;

import com.promsearch.prompt.application.port.out.user.PromoteUserGradePort;
import com.promsearch.user.domain.enums.GradeRequestStatus;
import com.promsearch.user.domain.enums.UserStatus;
import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import com.promsearch.user.infrastructure.persistence.entity.GradeRequestJpaEntity;
import com.promsearch.user.infrastructure.persistence.entity.UserJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserGradePromotionPersistenceAdapter implements PromoteUserGradePort {

    private final UserRepository userRepository;
    private final GradeRequestRepository gradeRequestRepository;

    @Override
    public void promoteForPostCreation(Long userId) {
        UserJpaEntity user = userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new UserDomainException(UserErrorCode.USER_NOT_FOUND));

        boolean reachedPrime = user.promoteGrade();
        userRepository.flush();

        if (reachedPrime && !gradeRequestRepository.existsByUserIdAndStatus(userId, GradeRequestStatus.PENDING)) {
            gradeRequestRepository.saveAndFlush(GradeRequestJpaEntity.createPendingOriginRequest(userId));
        }
    }
}
