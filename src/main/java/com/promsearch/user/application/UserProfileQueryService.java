package com.promsearch.user.application;

import com.promsearch.user.application.port.out.UserProfileStats;
import com.promsearch.user.application.port.out.UserProfileStatsReader;
import com.promsearch.user.application.port.out.UserRepository;
import com.promsearch.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileQueryService implements GetPublicUserProfileUseCase {

    private final UserRepository userRepository;
    private final UserProfileStatsReader userProfileStatsReader;

    @Override
    public PublicUserProfileInfo getProfile(Long userId) {
        User user = userRepository.getById(userId);
        UserProfileStats stats = userProfileStatsReader.getByUserId(userId);

        return PublicUserProfileInfo.from(user, stats);
    }
}
