package com.promsearch.admin.interfaces;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.promsearch.auth.application.usecase.AuthenticateAccessTokenUseCase;
import com.promsearch.user.application.usecase.ListGradeRequestsUseCase;
import com.promsearch.user.application.usecase.ProcessGradeRequestUseCase;
import com.promsearch.user.application.usecase.dto.GradeRequestListInfo;
import com.promsearch.user.application.usecase.dto.GradeRequestListQuery;
import com.promsearch.user.application.usecase.dto.GradeRequestSummaryInfo;
import com.promsearch.user.application.usecase.dto.ProcessGradeRequestCommand;
import com.promsearch.user.domain.enums.GradeRequestStatus;
import com.promsearch.user.domain.enums.UserGrade;
import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminGradeRequestController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminGradeRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticateAccessTokenUseCase authenticateAccessTokenUseCase;

    @MockitoBean
    private ListGradeRequestsUseCase listGradeRequestsUseCase;

    @MockitoBean
    private ProcessGradeRequestUseCase processGradeRequestUseCase;

    @DisplayName("심사 대기 목록을 조회하면 신청자 정보와 게시글/추천 수를 함께 반환한다")
    @Test
    void getGradeRequestsReturnsSummaries() throws Exception {
        GradeRequestSummaryInfo summary = new GradeRequestSummaryInfo(
                1L, 5L, "hanharam", "hanharam", UserGrade.PRIME, UserGrade.ORIGIN, GradeRequestStatus.PENDING,
                12L, 84L, Instant.parse("2026-07-23T12:00:00Z"), null
        );
        when(listGradeRequestsUseCase.list(new GradeRequestListQuery(GradeRequestStatus.PENDING, null, 0, 20)))
                .thenReturn(new GradeRequestListInfo(List.of(summary), 0, 20, 1, false));

        mockMvc.perform(get("/api/v1/admin/grade-requests").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[0].gradeRequestId").value(1))
                .andExpect(jsonPath("$.result.content[0].username").value("hanharam"))
                .andExpect(jsonPath("$.result.content[0].postCount").value(12))
                .andExpect(jsonPath("$.result.content[0].nickname").value("hanharam"))
                .andExpect(jsonPath("$.result.content[0].totalLikeCount").value(84))
                .andExpect(jsonPath("$.result.totalElements").value(1));
    }

    @DisplayName("승인 처리하면 처리 결과를 반환한다")
    @Test
    void processGradeRequestApproves() throws Exception {
        GradeRequestSummaryInfo processed = new GradeRequestSummaryInfo(
                1L, 5L, "hanharam", "hanharam", UserGrade.PRIME, UserGrade.ORIGIN, GradeRequestStatus.APPROVED,
                12L, 84L, Instant.parse("2026-07-23T12:00:00Z"), Instant.parse("2026-07-24T09:00:00Z")
        );
        when(processGradeRequestUseCase.process(new ProcessGradeRequestCommand(1L, GradeRequestStatus.APPROVED)))
                .thenReturn(processed);

        mockMvc.perform(patch("/api/v1/admin/grade-requests/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision":"APPROVED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("APPROVED"));

        verify(processGradeRequestUseCase).process(new ProcessGradeRequestCommand(1L, GradeRequestStatus.APPROVED));
    }

    @DisplayName("이미 처리된 항목을 다시 처리하면 409를 반환한다")
    @Test
    void processGradeRequestRejectsAlreadyProcessed() throws Exception {
        when(processGradeRequestUseCase.process(any()))
                .thenThrow(new UserDomainException(UserErrorCode.GRADE_REQUEST_ALREADY_PROCESSED));

        mockMvc.perform(patch("/api/v1/admin/grade-requests/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision":"APPROVED"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER-022"));
    }

    @DisplayName("등급업 신청 처리 결과는 PENDING일 수 없다")
    @Test
    void decisionCannotBePending() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/grade-requests/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision":"PENDING"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-001"));
    }

    @DisplayName("status 값이 유효하지 않으면 400을 반환한다")
    @Test
    void getGradeRequestsRejectsInvalidStatus() throws Exception {
        mockMvc.perform(get("/api/v1/admin/grade-requests").param("status", "DONE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-001"));
    }
}
