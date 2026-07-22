package com.promsearch.auth.domain;

import com.promsearch.auth.domain.enums.SocialProvider;

public class SocialAccount {

    private final Long id;
    private final Long userId;
    private final SocialProvider provider;
    private final String providerUserId;

    private SocialAccount(Long id, Long userId, SocialProvider provider, String providerUserId) {
        this.id = id;
        this.userId = userId;
        this.provider = provider;
        this.providerUserId = providerUserId;
    }

    public static SocialAccount create(Long userId, SocialProvider provider, String providerUserId) {
        return new SocialAccount(null, userId, provider, providerUserId);
    }

    public static SocialAccount reconstruct(Long id, Long userId, SocialProvider provider, String providerUserId) {
        return new SocialAccount(id, userId, provider, providerUserId);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public SocialProvider getProvider() {
        return provider;
    }

    public String getProviderUserId() {
        return providerUserId;
    }
}
