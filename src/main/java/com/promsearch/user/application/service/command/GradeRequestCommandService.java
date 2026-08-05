package com.promsearch.user.application.service.command;

import com.promsearch.user.application.port.out.graderequest.LoadGradeRequestPort;
import com.promsearch.user.application.port.out.graderequest.LoadGradeRequestSummaryPort;
import com.promsearch.user.application.port.out.graderequest.SaveGradeRequestPort;
import com.promsearch.user.application.port.out.user.LoadUserPort;
import com.promsearch.user.application.port.out.user.SaveUserPort;
import com.promsearch.user.application.usecase.ProcessGradeRequestUseCase;
import com.promsearch.user.application.usecase.dto.GradeRequestSummaryInfo;
import com.promsearch.user.application.usecase.dto.ProcessGradeRequestCommand;
import com.promsearch.user.domain.GradeRequest;
import com.promsearch.user.domain.User;
import com.promsearch.user.domain.enums.GradeRequestStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class GradeRequestCommandService implements ProcessGradeRequestUseCase {

    private final LoadGradeRequestPort loadGradeRequestPort;
    private final SaveGradeRequestPort saveGradeRequestPort;
    private final LoadGradeRequestSummaryPort loadGradeRequestSummaryPort;
    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;

    @Override
    public GradeRequestSummaryInfo process(ProcessGradeRequestCommand command) {
        GradeRequest gradeRequest = loadGradeRequestPort.getById(command.gradeRequestId());
        GradeRequest processed = gradeRequest.process(command.decision());
        saveGradeRequestPort.update(processed);

        if (command.decision() == GradeRequestStatus.APPROVED) {
            User user = loadUserPort.getById(gradeRequest.getUserId());
            saveUserPort.update(user.promoteToOrigin());
        }

        return loadGradeRequestSummaryPort.getById(processed.getGradeRequestId());
    }
}
