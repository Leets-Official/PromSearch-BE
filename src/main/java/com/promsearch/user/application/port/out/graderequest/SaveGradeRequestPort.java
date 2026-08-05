package com.promsearch.user.application.port.out.graderequest;

import com.promsearch.user.domain.GradeRequest;

public interface SaveGradeRequestPort {

    GradeRequest update(GradeRequest gradeRequest);
}
