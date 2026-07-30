package com.promsearch.prompt.application.port.out.storage;

import java.util.Arrays;

/** 원본 이미지 바이너리 조회 포트 */
public interface LoadPromptImageBinaryPort {

    /** Object Key에 저장된 이미지와 실제 Content-Type 반환 */
    StoredImage load(String objectKey);

    record StoredImage(byte[] bytes, String contentType) {

        public StoredImage {
            if (bytes == null || bytes.length == 0) {
                throw new IllegalArgumentException("원본 이미지 바이너리는 필수입니다.");
            }
            if (contentType == null || contentType.isBlank()) {
                throw new IllegalArgumentException("원본 이미지 Content-Type은 필수입니다.");
            }
            bytes = Arrays.copyOf(bytes, bytes.length);
            contentType = contentType.trim();
        }

        @Override
        public byte[] bytes() {
            return Arrays.copyOf(bytes, bytes.length);
        }
    }
}
