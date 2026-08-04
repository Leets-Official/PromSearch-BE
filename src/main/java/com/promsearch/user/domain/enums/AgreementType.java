package com.promsearch.user.domain.enums;

public enum AgreementType {
    SERVICE_TERMS(true, "2026-08-05"),
    COMMUNITY_TERMS(true, "2026-08-05"),
    CONTENT_POLICY(true, "2026-08-05"),
    AGE_14_OR_OVER(true, "2026-08-05"),
    MARKETING(false, "2026-08-05");

    private final boolean required;
    private final String version;

    AgreementType(boolean required, String version) {
        this.required = required;
        this.version = version;
    }

    public boolean isRequired() {
        return required;
    }

    public String getVersion() {
        return version;
    }
}
