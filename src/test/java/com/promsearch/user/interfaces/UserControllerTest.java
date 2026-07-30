package com.promsearch.user.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.promsearch.auth.application.usecase.AuthenticateAccessTokenUseCase;
import com.promsearch.user.application.usecase.CheckNicknameAvailabilityUseCase;
import com.promsearch.user.application.usecase.ChangePasswordUseCase;
import com.promsearch.user.application.usecase.DeleteUserUseCase;
import com.promsearch.user.application.usecase.UpdateUserProfileUseCase;
import com.promsearch.user.application.usecase.dto.NicknameAvailabilityInfo;
import com.promsearch.user.application.usecase.dto.NicknameAvailabilityQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticateAccessTokenUseCase authenticateAccessTokenUseCase;

    @MockitoBean
    private CheckNicknameAvailabilityUseCase checkNicknameAvailabilityUseCase;

    @MockitoBean
    private UpdateUserProfileUseCase updateUserProfileUseCase;

    @MockitoBean
    private ChangePasswordUseCase changePasswordUseCase;

    @MockitoBean
    private DeleteUserUseCase deleteUserUseCase;

    @DisplayName("사용 가능한 닉네임이면 available true를 반환한다")
    @Test
    void checkNicknameAvailabilityReturnsTrue() throws Exception {
        when(checkNicknameAvailabilityUseCase.checkAvailability(any(NicknameAvailabilityQuery.class)))
                .thenReturn(new NicknameAvailabilityInfo(true));

        mockMvc.perform(get("/api/v1/users/nicknames/availability")
                        .param("nickname", "new-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.available").value(true));
    }

    @DisplayName("닉네임이 비어 있으면 요청을 거부한다")
    @Test
    void checkNicknameAvailabilityRejectsBlankNickname() throws Exception {
        mockMvc.perform(get("/api/v1/users/nicknames/availability")
                        .param("nickname", " "))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("닉네임이 100자를 초과하면 요청을 거부한다")
    @Test
    void checkNicknameAvailabilityRejectsLongNickname() throws Exception {
        mockMvc.perform(get("/api/v1/users/nicknames/availability")
                        .param("nickname", "a".repeat(101)))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("내 프로필 조회는 가짜 성공 대신 구현 중 응답을 반환한다")
    @Test
    void getMyProfileReturnsNotImplemented() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-501"));
    }
}
