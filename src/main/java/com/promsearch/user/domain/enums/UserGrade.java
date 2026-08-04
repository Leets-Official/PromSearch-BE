package com.promsearch.user.domain.enums;

import java.util.Optional;

public enum UserGrade {
    NODE,
    LINK,
    SYNC,
    CORE,
    PRIME,
    ORIGIN;

    /**
     * 게시글 작성에 따른 자동 승급 시 다음 등급을 반환한다. Origin은 자동 승급 대상이 아니므로
     * Prime 이상에서는 항상 빈 값을 반환한다.
     */
    public Optional<UserGrade> nextAutoPromotionGrade() {
        return switch (this) {
            case NODE -> Optional.of(LINK);
            case LINK -> Optional.of(SYNC);
            case SYNC -> Optional.of(CORE);
            case CORE -> Optional.of(PRIME);
            case PRIME, ORIGIN -> Optional.empty();
        };
    }
}
