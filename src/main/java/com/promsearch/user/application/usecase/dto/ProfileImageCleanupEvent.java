package com.promsearch.user.application.usecase.dto;

/**
 * DB 커밋 후 더 이상 참조되지 않는 프로필 이미지 객체를 정리하기 위한 이벤트입니다.
 *
 * @param objectKey 삭제 대상 Object Key
 */
public record ProfileImageCleanupEvent(String objectKey) {

    public ProfileImageCleanupEvent {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("삭제할 프로필 이미지 Object Key는 필수입니다.");
        }
        objectKey = objectKey.trim();
    }
}
