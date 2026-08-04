package com.promsearch.user.domain.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserGradeTest {

    @DisplayName("Node부터 Core까지는 다음 등급으로 1단계씩 자동 승급된다")
    @Test
    void autoPromotionChain() {
        assertThat(UserGrade.NODE.nextAutoPromotionGrade()).contains(UserGrade.LINK);
        assertThat(UserGrade.LINK.nextAutoPromotionGrade()).contains(UserGrade.SYNC);
        assertThat(UserGrade.SYNC.nextAutoPromotionGrade()).contains(UserGrade.CORE);
        assertThat(UserGrade.CORE.nextAutoPromotionGrade()).contains(UserGrade.PRIME);
    }

    @DisplayName("Prime과 Origin은 자동 승급 대상이 아니다")
    @Test
    void noAutoPromotionBeyondPrime() {
        assertThat(UserGrade.PRIME.nextAutoPromotionGrade()).isEmpty();
        assertThat(UserGrade.ORIGIN.nextAutoPromotionGrade()).isEmpty();
    }
}
