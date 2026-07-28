package com.promsearch.admin.interfaces;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.promsearch.auth.application.usecase.AuthenticateAccessTokenUseCase;
import com.promsearch.global.config.security.MethodSecurityConfig;
import com.promsearch.moderation.application.usecase.SearchReportsUseCase;
import com.promsearch.moderation.application.usecase.UpdateReportStatusUseCase;
import com.promsearch.moderation.application.usecase.dto.ReportInfo;
import com.promsearch.moderation.application.usecase.dto.ReportPageInfo;
import com.promsearch.moderation.application.usecase.dto.SearchReportsQuery;
import com.promsearch.moderation.application.usecase.dto.UpdateReportStatusCommand;
import com.promsearch.moderation.domain.enums.ReportReason;
import com.promsearch.moderation.domain.enums.ReportStatus;
import com.promsearch.moderation.domain.enums.ReportTargetType;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityConfig.class)
class AdminReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticateAccessTokenUseCase authenticateAccessTokenUseCase;

    @MockitoBean
    private SearchReportsUseCase searchReportsUseCase;

    @MockitoBean
    private UpdateReportStatusUseCase updateReportStatusUseCase;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @DisplayName("관리자가 아니면 신고 목록 조회를 거절한다")
    @Test
    void getReportsRejectsNonAdmin() throws Exception {
        authenticateAs("USER");

        mockMvc.perform(get("/api/v1/admin/reports"))
                .andExpect(status().isForbidden());
    }

    @DisplayName("관리자는 신고 목록을 필터·페이지네이션으로 조회한다")
    @Test
    void getReportsReturnsSummariesForAdmin() throws Exception {
        authenticateAs("ADMIN");
        Instant createdAt = Instant.parse("2026-07-23T12:00:00Z");
        given(searchReportsUseCase.searchReports(SearchReportsQuery.of(ReportTargetType.POST, ReportStatus.PENDING, 0, 20)))
                .willReturn(new ReportPageInfo(
                        List.of(new ReportInfo(1L, ReportTargetType.POST, 10L, ReportReason.SPAM, "설명", ReportStatus.PENDING, 5L, createdAt)),
                        1L
                ));

        mockMvc.perform(get("/api/v1/admin/reports")
                        .param("targetType", "POST")
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result.content[0].reportId").value(1))
                .andExpect(jsonPath("$.result.content[0].targetType").value("POST"))
                .andExpect(jsonPath("$.result.content[0].reason").value("SPAM"))
                .andExpect(jsonPath("$.result.totalElements").value(1));
    }

    @DisplayName("targetType 값이 유효하지 않으면 400을 반환한다")
    @Test
    void getReportsRejectsInvalidTargetType() throws Exception {
        authenticateAs("ADMIN");

        mockMvc.perform(get("/api/v1/admin/reports").param("targetType", "USER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-001"));
    }

    @DisplayName("관리자가 아니면 신고 처리를 거절한다")
    @Test
    void updateReportStatusRejectsNonAdmin() throws Exception {
        authenticateAs("USER");

        mockMvc.perform(patch("/api/v1/admin/reports/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"RESOLVED"}
                                """))
                .andExpect(status().isForbidden());
    }

    @DisplayName("관리자는 신고를 RESOLVED로 처리할 수 있다")
    @Test
    void updateReportStatusResolvesReport() throws Exception {
        authenticateAs("ADMIN");
        Instant createdAt = Instant.parse("2026-07-23T12:00:00Z");
        given(updateReportStatusUseCase.updateStatus(UpdateReportStatusCommand.of(1L, ReportStatus.RESOLVED)))
                .willReturn(new ReportInfo(1L, ReportTargetType.POST, 10L, ReportReason.SPAM, "설명", ReportStatus.RESOLVED, 5L, createdAt));

        mockMvc.perform(patch("/api/v1/admin/reports/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"RESOLVED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("RESOLVED"));
    }

    @DisplayName("신고 처리 상태는 PENDING으로 되돌릴 수 없다")
    @Test
    void reportStatusCannotBeSetBackToPending() throws Exception {
        authenticateAs("ADMIN");

        mockMvc.perform(patch("/api/v1/admin/reports/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"PENDING"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-001"));
    }

    private void authenticateAs(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "test-principal",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        ));
    }
}
