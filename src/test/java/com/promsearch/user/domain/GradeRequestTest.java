package com.promsearch.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.promsearch.user.domain.enums.GradeRequestStatus;
import com.promsearch.user.domain.enums.UserGrade;
import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GradeRequestTest {

    @DisplayName("Prime 심사 대기 항목은 Prime에서 Origin으로의 PENDING 상태로 생성된다")
    @Test
    void createPendingOriginRequest() {
        GradeRequest gradeRequest = GradeRequest.createPendingOriginRequest(1L);

        assertThat(gradeRequest.getUserId()).isEqualTo(1L);
        assertThat(gradeRequest.getCurrentGrade()).isEqualTo(UserGrade.PRIME);
        assertThat(gradeRequest.getRequestedGrade()).isEqualTo(UserGrade.ORIGIN);
        assertThat(gradeRequest.getStatus()).isEqualTo(GradeRequestStatus.PENDING);
    }

    @DisplayName("유효하지 않은 사용자 식별자로는 심사 대기 항목을 생성할 수 없다")
    @Test
    void rejectsInvalidUserId() {
        assertThatThrownBy(() -> GradeRequest.createPendingOriginRequest(0L))
                .isInstanceOf(UserDomainException.class)
                .extracting("baseCode")
                .isEqualTo(UserErrorCode.INVALID_ID);
    }

    @DisplayName("PENDING 항목을 승인하면 APPROVED로 전환되고 처리 시각이 기록된다")
    @Test
    void processApprovesPendingRequest() {
        GradeRequest gradeRequest = GradeRequest.createPendingOriginRequest(1L);

        GradeRequest processed = gradeRequest.process(GradeRequestStatus.APPROVED);

        assertThat(processed.getStatus()).isEqualTo(GradeRequestStatus.APPROVED);
        assertThat(processed.getProcessedAt()).isNotNull();
        assertThat(processed.getUserId()).isEqualTo(gradeRequest.getUserId());
    }

    @DisplayName("PENDING 항목을 반려하면 REJECTED로 전환된다")
    @Test
    void processRejectsPendingRequest() {
        GradeRequest gradeRequest = GradeRequest.createPendingOriginRequest(1L);

        GradeRequest processed = gradeRequest.process(GradeRequestStatus.REJECTED);

        assertThat(processed.getStatus()).isEqualTo(GradeRequestStatus.REJECTED);
        assertThat(processed.getProcessedAt()).isNotNull();
    }

    @DisplayName("이미 처리된 항목은 다시 처리할 수 없다")
    @Test
    void rejectsProcessingAlreadyProcessedRequest() {
        GradeRequest processed = GradeRequest.createPendingOriginRequest(1L).process(GradeRequestStatus.APPROVED);

        assertThatThrownBy(() -> processed.process(GradeRequestStatus.REJECTED))
                .isInstanceOf(UserDomainException.class)
                .extracting("baseCode")
                .isEqualTo(UserErrorCode.GRADE_REQUEST_ALREADY_PROCESSED);
    }
}
