package com.promsearch.user.application.usecase.dto;

import java.net.URI;
import java.time.Instant;

/**
 * 업로드 URL 발급 UseCase가 인터페이스 계층에 반환하는 결과.
 *
 * @param objectKey 완료 요청에서 다시 제출해야 하는 서버 생성 객체 키
 * @param uploadUrl 클라이언트가 이미지 본문을 PUT으로 전송할 URL
 * @param expiresAt 업로드 URL 만료 시각
 */
public record ProfileImageUploadUrlInfo(
        String objectKey,
        URI uploadUrl,
        Instant expiresAt
) {
}
