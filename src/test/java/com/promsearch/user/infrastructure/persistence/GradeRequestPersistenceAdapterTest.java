package com.promsearch.user.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.promsearch.global.config.JpaConfig;
import com.promsearch.user.domain.GradeRequest;
import com.promsearch.user.domain.enums.GradeRequestStatus;
import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import com.promsearch.user.infrastructure.persistence.entity.GradeRequestJpaEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({
        JpaConfig.class,
        GradeRequestPersistenceAdapter.class
})
class GradeRequestPersistenceAdapterTest {

    @Autowired
    private GradeRequestPersistenceAdapter adapter;

    @Autowired
    private GradeRequestRepository gradeRequestRepository;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("식별자로 조회한다")
    @Test
    void getByIdReturnsGradeRequest() {
        GradeRequestJpaEntity entity = gradeRequestRepository.saveAndFlush(
                GradeRequestJpaEntity.createPendingOriginRequest(1L)
        );
        entityManager.clear();

        GradeRequest gradeRequest = adapter.getById(entity.getId());

        assertThat(gradeRequest.getUserId()).isEqualTo(1L);
        assertThat(gradeRequest.getStatus()).isEqualTo(GradeRequestStatus.PENDING);
    }

    @DisplayName("존재하지 않는 식별자를 조회하면 예외가 발생한다")
    @Test
    void getByIdThrowsWhenMissing() {
        assertThatThrownBy(() -> adapter.getById(9_999L))
                .isInstanceOf(UserDomainException.class)
                .extracting("baseCode")
                .isEqualTo(UserErrorCode.GRADE_REQUEST_NOT_FOUND);
    }

    @DisplayName("처리 상태와 처리 시각을 갱신한다")
    @Test
    void updatePersistsProcessedState() {
        GradeRequestJpaEntity entity = gradeRequestRepository.saveAndFlush(
                GradeRequestJpaEntity.createPendingOriginRequest(1L)
        );
        entityManager.clear();

        GradeRequest processed = adapter.getById(entity.getId()).process(GradeRequestStatus.APPROVED);
        adapter.update(processed);
        entityManager.clear();

        GradeRequest reloaded = adapter.getById(entity.getId());
        assertThat(reloaded.getStatus()).isEqualTo(GradeRequestStatus.APPROVED);
        assertThat(reloaded.getProcessedAt()).isNotNull();
    }
}
