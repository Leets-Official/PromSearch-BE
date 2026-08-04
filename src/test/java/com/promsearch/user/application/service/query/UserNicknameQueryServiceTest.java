package com.promsearch.user.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.promsearch.user.application.port.out.user.LoadUserPort;
import com.promsearch.user.application.usecase.dto.NicknameAvailabilityInfo;
import com.promsearch.user.application.usecase.dto.NicknameAvailabilityQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserNicknameQueryServiceTest {

    private final LoadUserPort loadUserPort = mock(LoadUserPort.class);
    private final UserNicknameQueryService service = new UserNicknameQueryService(loadUserPort);

    @DisplayName("존재하지 않는 닉네임은 사용 가능하다")
    @Test
    void nicknameIsAvailableWhenItDoesNotExist() {
        when(loadUserPort.existsByNickname("new-user")).thenReturn(false);

        NicknameAvailabilityInfo info = service.checkAvailability(NicknameAvailabilityQuery.of("new-user"));

        assertThat(info.available()).isTrue();
        verify(loadUserPort).existsByNickname("new-user");
    }

    @DisplayName("이미 존재하는 닉네임은 사용할 수 없다")
    @Test
    void nicknameIsUnavailableWhenItExists() {
        when(loadUserPort.existsByNickname("existing-user")).thenReturn(true);

        NicknameAvailabilityInfo info = service.checkAvailability(NicknameAvailabilityQuery.of("existing-user"));

        assertThat(info.available()).isFalse();
        verify(loadUserPort).existsByNickname("existing-user");
    }
}
