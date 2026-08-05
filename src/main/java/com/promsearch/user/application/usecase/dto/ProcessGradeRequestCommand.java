package com.promsearch.user.application.usecase.dto;

import com.promsearch.user.domain.enums.GradeRequestStatus;

public record ProcessGradeRequestCommand(Long gradeRequestId, GradeRequestStatus decision) {

    public ProcessGradeRequestCommand {
        if (gradeRequestId == null || gradeRequestId <= 0) {
            throw new IllegalArgumentException("gradeRequestId must be greater than 0");
        }
        if (decision != GradeRequestStatus.APPROVED && decision != GradeRequestStatus.REJECTED) {
            throw new IllegalArgumentException("decision must be APPROVED or REJECTED");
        }
    }
}
