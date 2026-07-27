package com.promsearch.worker.prompt.infrastructure.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.promsearch.prompt.application.port.out.storage.RenderPromptImageWatermarkPort.RenderedImage;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import com.promsearch.worker.prompt.infrastructure.image.Java2dPromptImageWatermarkRenderer.WatermarkPlacement;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class Java2dPromptImageWatermarkRendererTest {

    private Java2dPromptImageWatermarkRenderer renderer;

    @BeforeEach
    void setUp() {
        WatermarkRenderingProperties properties = new WatermarkRenderingProperties(
                1,
                new ClassPathResource("watermark/wordmark.png"),
                "#6B7280",
                0.26,
                0.095,
                80,
                240,
                0.123,
                0.21,
                0.018,
                0.028,
                0.92,
                10 * 1024 * 1024
        );
        renderer = new Java2dPromptImageWatermarkRenderer(properties);
        renderer.loadLogo();
    }

    @DisplayName("투명 PNG에 wordmark를 반복 합성하고 원본 크기와 알파 채널을 유지한다")
    @Test
    void renderPng() throws Exception {
        byte[] source = createImage("png", 640, 360, true);

        RenderedImage rendered = renderer.render(source, "image/png", 640, 360);
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(rendered.bytes()));

        assertThat(rendered.contentType()).isEqualTo("image/png");
        assertThat(decoded.getWidth()).isEqualTo(640);
        assertThat(decoded.getHeight()).isEqualTo(360);
        assertThat(decoded.getColorModel().hasAlpha()).isTrue();
        assertThat(rendered.bytes()).isNotEqualTo(source);
    }

    @DisplayName("JPEG 원본을 같은 크기의 JPEG 워터마크 결과로 인코딩한다")
    @Test
    void renderJpeg() throws Exception {
        byte[] source = createImage("jpeg", 800, 450, false);

        RenderedImage rendered = renderer.render(source, "image/jpeg", 800, 450);
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(rendered.bytes()));

        assertThat(rendered.contentType()).isEqualTo("image/jpeg");
        assertThat(decoded.getWidth()).isEqualTo(800);
        assertThat(decoded.getHeight()).isEqualTo(450);
        assertThat(decoded.getColorModel().hasAlpha()).isFalse();
    }

    @DisplayName("흰 배경에서도 회색 wordmark를 식별할 수 있다")
    @Test
    void renderGrayWatermarkOnWhiteBackground() throws Exception {
        byte[] source = createImage("png", 640, 360, false, Color.WHITE);

        RenderedImage rendered = renderer.render(source, "image/png", 640, 360);
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(rendered.bytes()));

        boolean containsVisibleWatermark = false;
        for (int y = 0; y < decoded.getHeight() && !containsVisibleWatermark; y++) {
            for (int x = 0; x < decoded.getWidth(); x++) {
                Color pixel = new Color(decoded.getRGB(x, y), true);
                if (pixel.getRed() < 250
                        && pixel.getGreen() < 250
                        && pixel.getBlue() < 250) {
                    containsVisibleWatermark = true;
                    break;
                }
            }
        }

        assertThat(containsVisibleWatermark).isTrue();
    }

    @DisplayName("디코딩한 이미지 크기가 DB 예상값과 다르면 처리를 거절한다")
    @Test
    void rejectUnexpectedDimensions() throws Exception {
        byte[] source = createImage("png", 640, 360, true);

        assertThatThrownBy(() -> renderer.render(source, "image/png", 320, 180))
                .isInstanceOf(PromptDomainException.class)
                .extracting("baseCode")
                .isEqualTo(PromptErrorCode.INVALID_IMAGE_SOURCE);
    }

    @DisplayName("1280x720 기준 시안처럼 5개와 4개가 교차하는 다섯 행을 배치한다")
    @Test
    void calculateReferenceLayout() {
        List<WatermarkPlacement> placements = renderer.calculatePlacements(1_280, 720);

        assertThat(placements).hasSize(23);
        assertThat(placements.stream().filter(placement -> placement.y() == 20).count())
                .isEqualTo(5);
        assertThat(placements.stream().filter(placement -> placement.y() == 186).count())
                .isEqualTo(4);
    }

    private byte[] createImage(
            String format,
            int width,
            int height,
            boolean alpha
    ) throws Exception {
        return createImage(format, width, height, alpha, new Color(20, 60, 100, alpha ? 120 : 255));
    }

    private byte[] createImage(
            String format,
            int width,
            int height,
            boolean alpha,
            Color color
    ) throws Exception {
        BufferedImage image = new BufferedImage(
                width,
                height,
                alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB
        );
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(color);
            graphics.fillRect(0, 0, width, height);
        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, format, output);
            return output.toByteArray();
        }
    }
}
