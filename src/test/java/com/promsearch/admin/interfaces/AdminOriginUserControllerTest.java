package com.promsearch.admin.interfaces;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.promsearch.auth.application.usecase.AuthenticateAccessTokenUseCase;
import com.promsearch.user.application.usecase.ListOriginUsersUseCase;
import com.promsearch.user.application.usecase.dto.OriginUserListInfo;
import com.promsearch.user.application.usecase.dto.OriginUserListQuery;
import com.promsearch.user.application.usecase.dto.OriginUserSummaryInfo;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminOriginUserController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminOriginUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticateAccessTokenUseCase authenticateAccessTokenUseCase;

    @MockitoBean
    private ListOriginUsersUseCase listOriginUsersUseCase;

    @DisplayName("Origin 등급 유저 목록을 조회한다")
    @Test
    void getOriginUsersReturnsSummaries() throws Exception {
        when(listOriginUsersUseCase.list(new OriginUserListQuery(0, 20)))
                .thenReturn(new OriginUserListInfo(List.of(new OriginUserSummaryInfo(5L, "hanharam")), 0, 20, 1, false));

        mockMvc.perform(get("/api/v1/admin/origin-users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[0].userId").value(5))
                .andExpect(jsonPath("$.result.content[0].username").value("hanharam"))
                .andExpect(jsonPath("$.result.totalElements").value(1));
    }

    @DisplayName("size 값이 상한을 초과하면 400을 반환한다")
    @Test
    void rejectsSizeAboveLimit() throws Exception {
        mockMvc.perform(get("/api/v1/admin/origin-users").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-400"));
    }
}
