package com.promsearch.auth.application;

import com.promsearch.auth.application.port.out.AccessTokenProvider;
import com.promsearch.auth.application.port.out.RefreshTokenProvider;
import com.promsearch.auth.domain.exception.AuthDomainException;
import com.promsearch.auth.domain.exception.AuthErrorCode;
import com.promsearch.user.application.port.out.UserRepository;
import com.promsearch.user.domain.User;
import com.promsearch.user.domain.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthCommandService implements LoginUseCase, ReissueUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenProvider accessTokenProvider;
    private final RefreshTokenProvider refreshTokenProvider;

    @Override
    public LoginInfo login(LoginCommand command) {
        User user = userRepository.findByEmail(command.email())
                .orElseThrow(() -> new AuthDomainException(AuthErrorCode.INVALID_CREDENTIALS));

        validatePassword(command.password(), user.getPassword());
        validateActiveUser(user);

        return LoginInfo.of(
                accessTokenProvider.createAccessToken(user),
                refreshTokenProvider.createRefreshToken(user),
                accessTokenProvider.getAccessTokenExpirationSeconds(),
                user
        );
    }

    @Override
    public ReissueInfo reissue(ReissueCommand command) {
        Long userId = refreshTokenProvider.getUserId(command.refreshToken());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthDomainException(AuthErrorCode.INVALID_TOKEN));

        validateActiveUser(user);

        return ReissueInfo.of(
                accessTokenProvider.createAccessToken(user),
                accessTokenProvider.getAccessTokenExpirationSeconds()
        );
    }

    private void validatePassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new AuthDomainException(AuthErrorCode.INVALID_CREDENTIALS);
        }
    }

    private void validateActiveUser(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthDomainException(AuthErrorCode.INVALID_CREDENTIALS);
        }
    }
}
