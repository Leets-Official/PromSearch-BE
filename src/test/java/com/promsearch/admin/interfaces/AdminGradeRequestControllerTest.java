package com.promsearch.admin.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.promsearch.auth.application.usecase.AuthenticateAccessTokenUseCase;
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

    @DisplayName("등급업 신청 목록 조회·처리는 가짜 성공 대신 구현 중 응답을 반환한다")
    @Test
    void gradeRequestEndpointsReturnNotImplemented() throws Exception {
        mockMvc.perform(get("/api/v1/admin/grade-requests"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-501"));

        mockMvc.perform(patch("/api/v1/admin/grade-requests/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision":"APPROVED"}
                                """))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.code").value("COMMON-501"));
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
