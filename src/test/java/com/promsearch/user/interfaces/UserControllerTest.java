package com.promsearch.user.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

import com.promsearch.auth.application.usecase.AuthenticateAccessTokenUseCase;
import com.promsearch.user.application.usecase.ChangePasswordUseCase;
import com.promsearch.user.application.usecase.DeleteUserUseCase;
import com.promsearch.user.application.usecase.GetPublicUserProfileUseCase;
import com.promsearch.user.application.usecase.UpdateUserProfileUseCase;
import com.promsearch.user.application.usecase.dto.PublicUserProfileInfo;
import com.promsearch.user.domain.enums.UserGrade;
import java.time.Instant;
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
    private UpdateUserProfileUseCase updateUserProfileUseCase;

    @MockitoBean
    private ChangePasswordUseCase changePasswordUseCase;

    @MockitoBean
    private DeleteUserUseCase deleteUserUseCase;

    @MockitoBean
    private GetPublicUserProfileUseCase getPublicUserProfileUseCase;

    @DisplayName("내 프로필 조회는 가짜 성공 대신 구현 중 응답을 반환한다")
    @Test
    void getMyProfileReturnsNotImplemented() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-501"));
    }

    @DisplayName("공개 프로필 조회는 실명을 응답하지 않는다")
    @Test
    void getPublicProfileDoesNotExposeRealName() throws Exception {
        when(getPublicUserProfileUseCase.getProfile(12L)).thenReturn(new PublicUserProfileInfo(
                12L,
                "prompt-maker",
                "https://cdn.promsearch.com/profiles/12.jpg",
                UserGrade.PRIME,
                8,
                124,
                2_300,
                Instant.parse("2026-07-23T12:00:00Z")
        ));

        mockMvc.perform(get("/api/v1/users/12/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.userId").value(12))
                .andExpect(jsonPath("$.result.nickname").value("prompt-maker"))
                .andExpect(jsonPath("$.result.name").doesNotExist());
    }
}
