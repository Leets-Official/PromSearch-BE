package com.promsearch.user.infrastructure.persistence;

import com.promsearch.user.application.port.out.graderequest.LoadGradeRequestPort;
import com.promsearch.user.application.port.out.graderequest.SaveGradeRequestPort;
import com.promsearch.user.domain.GradeRequest;
import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import com.promsearch.user.infrastructure.persistence.entity.GradeRequestJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GradeRequestPersistenceAdapter implements LoadGradeRequestPort, SaveGradeRequestPort {

    private final GradeRequestRepository gradeRequestRepository;

    @Override
    public GradeRequest getById(Long gradeRequestId) {
        return gradeRequestRepository.findById(gradeRequestId)
                .map(GradeRequestJpaEntity::toDomain)
                .orElseThrow(() -> new UserDomainException(UserErrorCode.GRADE_REQUEST_NOT_FOUND));
    }

    @Override
    public GradeRequest update(GradeRequest gradeRequest) {
        GradeRequestJpaEntity entity = gradeRequestRepository.findById(gradeRequest.getGradeRequestId())
                .orElseThrow(() -> new UserDomainException(UserErrorCode.GRADE_REQUEST_NOT_FOUND));
        entity.updateFrom(gradeRequest);
        gradeRequestRepository.flush();
        return entity.toDomain();
    }
}
