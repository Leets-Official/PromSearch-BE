package com.promsearch.user.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.promsearch.user.application.port.out.graderequest.LoadGradeRequestPort;
import com.promsearch.user.application.port.out.graderequest.LoadGradeRequestSummaryPort;
import com.promsearch.user.application.port.out.graderequest.SaveGradeRequestPort;
import com.promsearch.user.application.port.out.user.LoadUserPort;
import com.promsearch.user.application.port.out.user.SaveUserPort;
import com.promsearch.user.application.usecase.dto.GradeRequestSummaryInfo;
import com.promsearch.user.application.usecase.dto.ProcessGradeRequestCommand;
import com.promsearch.user.domain.GradeRequest;
import com.promsearch.user.domain.User;
import com.promsearch.user.domain.User.UserId;
import com.promsearch.user.domain.enums.GradeRequestStatus;
import com.promsearch.user.domain.enums.UserGrade;
import com.promsearch.user.domain.enums.UserRole;
import com.promsearch.user.domain.enums.UserStatus;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GradeRequestCommandServiceTest {

    @Mock
    private LoadGradeRequestPort loadGradeRequestPort;
    @Mock
    private SaveGradeRequestPort saveGradeRequestPort;
    @Mock
    private LoadGradeRequestSummaryPort loadGradeRequestSummaryPort;
    @Mock
    private LoadUserPort loadUserPort;
    @Mock
    private SaveUserPort saveUserPort;

    private GradeRequestCommandService service;

    private GradeRequestCommandService service() {
        return new GradeRequestCommandService(
                loadGradeRequestPort, saveGradeRequestPort, loadGradeRequestSummaryPort, loadUserPort, saveUserPort
        );
    }

    @DisplayName("승인하면 유저 등급을 Origin으로 변경하고 신청 상태를 갱신한다")
    @Test
    void approveGradeRequestPromotesUserToOrigin() {
        service = service();
        GradeRequest pending = GradeRequest.createPendingOriginRequest(5L);
        when(loadGradeRequestPort.getById(1L)).thenReturn(pending);
        when(loadUserPort.getById(5L)).thenReturn(primeUser());
        GradeRequestSummaryInfo expected = new GradeRequestSummaryInfo(
                1L, 5L, "hanharam", "hanharam", UserGrade.PRIME, UserGrade.ORIGIN, GradeRequestStatus.APPROVED,
                0L, 0L, Instant.now(), Instant.now()
        );
        when(loadGradeRequestSummaryPort.getById(any())).thenReturn(expected);

        GradeRequestSummaryInfo result = service.process(new ProcessGradeRequestCommand(1L, GradeRequestStatus.APPROVED));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(saveUserPort).update(userCaptor.capture());
        assertThat(userCaptor.getValue().getGrade()).isEqualTo(UserGrade.ORIGIN);

        ArgumentCaptor<GradeRequest> gradeRequestCaptor = ArgumentCaptor.forClass(GradeRequest.class);
        verify(saveGradeRequestPort).update(gradeRequestCaptor.capture());
        assertThat(gradeRequestCaptor.getValue().getStatus()).isEqualTo(GradeRequestStatus.APPROVED);

        assertThat(result).isEqualTo(expected);
    }

    @DisplayName("반려하면 유저 등급은 변경하지 않는다")
    @Test
    void rejectGradeRequestDoesNotChangeUserGrade() {
        service = service();
        GradeRequest pending = GradeRequest.createPendingOriginRequest(5L);
        when(loadGradeRequestPort.getById(1L)).thenReturn(pending);
        when(loadGradeRequestSummaryPort.getById(any())).thenReturn(new GradeRequestSummaryInfo(
                1L, 5L, "hanharam", "hanharam", UserGrade.PRIME, UserGrade.ORIGIN, GradeRequestStatus.REJECTED,
                0L, 0L, Instant.now(), Instant.now()
        ));

        service.process(new ProcessGradeRequestCommand(1L, GradeRequestStatus.REJECTED));

        verify(loadUserPort, never()).getById(any());
        verify(saveUserPort, never()).update(any());
    }

    private User primeUser() {
        Instant now = Instant.now();
        return User.reconstruct(
                new UserId(5L), "user@promsearch.com", "password", "nickname", "name",
                null, null, 0L, UserRole.USER, UserGrade.PRIME, UserStatus.ACTIVE, now, now
        );
    }
}
