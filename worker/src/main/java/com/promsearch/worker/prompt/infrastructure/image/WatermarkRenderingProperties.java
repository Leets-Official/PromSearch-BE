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
        String logoColor,
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
                || !isRgbHex(logoColor)
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

    /** 설정 문자열이 Java2D에서 사용할 수 있는 RGB HEX 형식인지 확인 */
    private static boolean isRgbHex(String value) {
        return value != null && value.matches("^#[0-9a-fA-F]{6}$");
    }

    /** 투명도·품질처럼 0 초과 1 이하인 비율인지 확인 */
    private static boolean isUnitRatio(double value) {
        return value > 0 && value <= 1;
    }

    /** 간격·여백처럼 0 이상 1 이하인 비율인지 확인 */
    private static boolean isNonNegativeRatio(double value) {
        return value >= 0 && value <= 1;
    }
}
