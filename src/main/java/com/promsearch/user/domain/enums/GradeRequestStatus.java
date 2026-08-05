package com.promsearch.user.domain.enums;

/**
 * Origin 등급업 심사 대기 항목의 처리 상태. 유저가 자동 승급으로 Prime에 도달하면 PENDING으로
 * 자동 생성되며, 관리자가 승인/반려하면 각각 APPROVED/REJECTED로 전환됩니다.
 */
public enum GradeRequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}
