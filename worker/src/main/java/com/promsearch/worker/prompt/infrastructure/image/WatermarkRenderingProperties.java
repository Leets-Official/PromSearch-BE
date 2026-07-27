package com.promsearch.worker.prompt.infrastructure.image;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

/** 디자인 확정 전후에 코드 수정 없이 조절할 수 있는 워터마크 시각 정책 */
@Validated
@ConfigurationProperties(prefix = "worker.watermark")
public record WatermarkRenderingProperties(
        int concurrency,
        @NotNull Resource logo,
        double opacity,
        double logoWidthRatio,
        int minimumLogoWidth,
        int maximumLogoWidth,
        double horizontalGapRatio,
        double verticalGapRatio,
        double horizontalMarginRatio,
        double verticalMarginRatio,
        double jpegQuality,
        long maximumOutputBytes
) {

    public WatermarkRenderingProperties {
        if (concurrency <= 0
                || !isUnitRatio(opacity)
                || !isUnitRatio(logoWidthRatio)
                || minimumLogoWidth <= 0
                || maximumLogoWidth < minimumLogoWidth
                || !isNonNegativeRatio(horizontalGapRatio)
                || !isNonNegativeRatio(verticalGapRatio)
                || !isNonNegativeRatio(horizontalMarginRatio)
                || !isNonNegativeRatio(verticalMarginRatio)
                || !isUnitRatio(jpegQuality)
                || maximumOutputBytes <= 0) {
            throw new IllegalArgumentException("워터마크 렌더링 설정이 유효하지 않습니다.");
        }
    }

    private static boolean isUnitRatio(double value) {
        return value > 0 && value <= 1;
    }

    private static boolean isNonNegativeRatio(double value) {
        return value >= 0 && value <= 1;
    }
}
