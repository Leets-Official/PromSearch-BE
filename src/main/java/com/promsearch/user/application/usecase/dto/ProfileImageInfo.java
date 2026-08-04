package com.promsearch.user.application.usecase.dto;

/**
 * 프로필에 최종 적용된 이미지 정보입니다.
 *
 * @param profileImageUrl 외부에서 조회할 수 있는 프로필 이미지 URL
 */
public record ProfileImageInfo(String profileImageUrl) {
}
