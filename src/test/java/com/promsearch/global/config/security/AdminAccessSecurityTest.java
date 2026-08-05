package com.promsearch.global.config.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AdminAccessSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @DisplayName("인증 없이 관리자 API를 호출하면 401을 반환한다")
    @Test
    void adminApiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/grade-requests"))
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("일반 USER 권한으로 관리자 API를 호출하면 403을 반환한다")
    @Test
    void adminApiRejectsNonAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/grade-requests").with(user("member").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @DisplayName("ADMIN 권한으로 관리자 API를 호출하면 통과한다")
    @Test
    void adminApiAllowsAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/origin-users").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }
}
