package com.promsearch.moderation.domain.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReportReasonTest {

    @DisplayName("COMMENT 대상은 COPYRIGHT, LOW_QUALITY 사유를 허용하지 않는다")
    @Test
    void commentTargetDisallowsCopyrightAndLowQuality() {
        assertThat(ReportReason.COPYRIGHT.isAllowedFor(ReportTargetType.COMMENT)).isFalse();
        assertThat(ReportReason.LOW_QUALITY.isAllowedFor(ReportTargetType.COMMENT)).isFalse();
        assertThat(ReportReason.SPAM.isAllowedFor(ReportTargetType.COMMENT)).isTrue();
        assertThat(ReportReason.INAPPROPRIATE.isAllowedFor(ReportTargetType.COMMENT)).isTrue();
        assertThat(ReportReason.ETC.isAllowedFor(ReportTargetType.COMMENT)).isTrue();
    }

    @DisplayName("POST 대상은 모든 신고 사유를 허용한다")
    @Test
    void postTargetAllowsAllReasons() {
        for (ReportReason reason : ReportReason.values()) {
            assertThat(reason.isAllowedFor(ReportTargetType.POST)).isTrue();
        }
    }

    @DisplayName("대상 타입이 null이면 허용하지 않는다")
    @Test
    void nullTargetTypeIsNotAllowed() {
        for (ReportReason reason : ReportReason.values()) {
            assertThat(reason.isAllowedFor(null)).isFalse();
        }
    }
}
