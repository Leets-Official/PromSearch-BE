package com.promsearch.moderation.domain.enums;

import java.util.EnumSet;
import java.util.Set;

public enum ReportReason {
    SPAM,
    INAPPROPRIATE,
    COPYRIGHT,
    LOW_QUALITY,
    ETC;

    private static final Set<ReportReason> COMMENT_DISALLOWED = EnumSet.of(COPYRIGHT, LOW_QUALITY);

    public boolean isAllowedFor(ReportTargetType targetType) {
        if (targetType == ReportTargetType.COMMENT) {
            return !COMMENT_DISALLOWED.contains(this);
        }
        return true;
    }
}
