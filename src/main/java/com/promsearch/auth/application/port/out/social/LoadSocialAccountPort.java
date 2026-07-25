package com.promsearch.auth.application.port.out.social;

import com.promsearch.auth.domain.SocialAccount;
import com.promsearch.auth.domain.enums.SocialProvider;
import java.util.Optional;

public interface LoadSocialAccountPort {

    Optional<SocialAccount> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);
}
