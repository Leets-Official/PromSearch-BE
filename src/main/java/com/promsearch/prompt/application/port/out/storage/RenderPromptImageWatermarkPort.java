package com.promsearch.prompt.application.port.out.storage;

import java.util.Arrays;

/** 이미지 형식과 워터마크 구현체를 격리하는 렌더링 포트 */
public interface RenderPromptImageWatermarkPort {

    /** 원본과 동일한 크기·형식으로 워터마크 결과 생성 */
    RenderedImage render(
            byte[] source,
            String contentType,
            int expectedWidth,
            int expectedHeight
    );

    record RenderedImage(byte[] bytes, String contentType, int width, int height) {

        public RenderedImage {
            if (bytes == null || bytes.length == 0) {
                throw new IllegalArgumentException("워터마크 결과 바이너리는 필수입니다.");
            }
            if (contentType == null || contentType.isBlank() || width <= 0 || height <= 0) {
                throw new IllegalArgumentException("워터마크 결과 메타데이터가 유효하지 않습니다.");
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
