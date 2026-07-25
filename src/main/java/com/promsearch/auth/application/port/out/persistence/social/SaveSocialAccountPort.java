package com.promsearch.auth.application.port.out.persistence.social;

import com.promsearch.auth.domain.SocialAccount;

public interface SaveSocialAccountPort {

    SocialAccount save(SocialAccount socialAccount);
}
