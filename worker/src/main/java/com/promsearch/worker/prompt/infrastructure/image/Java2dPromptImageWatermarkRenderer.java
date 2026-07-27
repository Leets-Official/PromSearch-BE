package com.promsearch.worker.prompt.infrastructure.image;

import com.promsearch.prompt.application.port.out.storage.RenderPromptImageWatermarkPort;
import com.promsearch.prompt.domain.PromptImage;
import com.promsearch.prompt.domain.enums.PromptImageContentType;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import jakarta.annotation.PostConstruct;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Java2D와 ImageIO만 사용해 작은 wordmark를 반복 배치하는 렌더러 */
@Component
@RequiredArgsConstructor
public class Java2dPromptImageWatermarkRenderer implements RenderPromptImageWatermarkPort {

    private final WatermarkRenderingProperties properties;
    private BufferedImage logo;

    /** 클래스패스 wordmark 자산을 Worker 시작 시 한 번 로드 */
    @PostConstruct
    void loadLogo() {
        try (var inputStream = properties.logo().getInputStream()) {
            logo = ImageIO.read(inputStream);
            if (logo == null || logo.getWidth() <= 0 || logo.getHeight() <= 0) {
                throw new IllegalStateException("워터마크 로고 이미지를 읽을 수 없습니다.");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("워터마크 로고 이미지를 불러올 수 없습니다.", exception);
        }
    }

    /** 디코딩 전 크기 제한 확인 후 원본과 같은 JPEG 또는 PNG로 출력 */
    @Override
    public RenderedImage render(
            byte[] source,
            String contentType,
            int expectedWidth,
            int expectedHeight
    ) {
        PromptImageContentType imageContentType =
                PromptImageContentType.fromMimeType(contentType);
        BufferedImage original = readAndValidate(source, expectedWidth, expectedHeight);
        BufferedImage watermarked = overlay(original, imageContentType);
        byte[] encoded = encode(watermarked, imageContentType);

        if (encoded.length > properties.maximumOutputBytes()) {
            throw new PromptDomainException(
                    PromptErrorCode.IMAGE_WATERMARK_RENDER_FAILED,
                    "워터마크 결과 이미지가 허용 크기를 초과했습니다."
            );
        }
        return new RenderedImage(
                encoded,
                imageContentType.getMimeType(),
                watermarked.getWidth(),
                watermarked.getHeight()
        );
    }

    private BufferedImage readAndValidate(byte[] source, int expectedWidth, int expectedHeight) {
        if (source == null || source.length == 0) {
            throw invalidSource("원본 이미지 데이터가 없습니다.", null);
        }

        try (ImageInputStream imageInput =
                     ImageIO.createImageInputStream(new ByteArrayInputStream(source))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw invalidSource("지원하는 이미지 디코더를 찾을 수 없습니다.", null);
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                // TODO: 모바일 JPEG의 EXIF Orientation 지원 범위 확정 시 디코딩 전 회전·반전 적용
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                validateDimensions(width, height, expectedWidth, expectedHeight);
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw invalidSource("원본 이미지를 디코딩할 수 없습니다.", null);
                }
                return image;
            } finally {
                reader.dispose();
            }
        } catch (PromptDomainException exception) {
            throw exception;
        } catch (IOException exception) {
            throw invalidSource("원본 이미지 디코딩에 실패했습니다.", exception);
        }
    }

    private void validateDimensions(
            int width,
            int height,
            int expectedWidth,
            int expectedHeight
    ) {
        if (width != expectedWidth
                || height != expectedHeight
                || width <= 0
                || height <= 0
                || width > PromptImage.MAX_DIMENSION
                || height > PromptImage.MAX_DIMENSION
                || (long) width * height > PromptImage.MAX_PIXEL_COUNT) {
            throw invalidSource("원본 이미지 크기가 이미지 자산과 일치하지 않습니다.", null);
        }
    }

    private BufferedImage overlay(
            BufferedImage original,
            PromptImageContentType contentType
    ) {
        int outputType = contentType == PromptImageContentType.PNG
                ? BufferedImage.TYPE_INT_ARGB
                : BufferedImage.TYPE_INT_RGB;
        BufferedImage output =
                new BufferedImage(original.getWidth(), original.getHeight(), outputType);
        Graphics2D graphics = output.createGraphics();
        try {
            configureQuality(graphics);
            if (contentType == PromptImageContentType.JPEG) {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, output.getWidth(), output.getHeight());
            }
            graphics.drawImage(original, 0, 0, null);
            drawRepeatedLogo(graphics, output.getWidth(), output.getHeight());
            return output;
        } catch (RuntimeException exception) {
            throw new PromptDomainException(
                    PromptErrorCode.IMAGE_WATERMARK_RENDER_FAILED,
                    "워터마크 합성에 실패했습니다.",
                    exception
            );
        } finally {
            graphics.dispose();
        }
    }

    private void drawRepeatedLogo(Graphics2D graphics, int width, int height) {
        List<WatermarkPlacement> placements = calculatePlacements(width, height);
        graphics.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER,
                (float) properties.opacity()
        ));
        for (WatermarkPlacement placement : placements) {
            graphics.drawImage(
                    logo,
                    placement.x(),
                    placement.y(),
                    placement.width(),
                    placement.height(),
                    null
            );
        }
    }

    /** 기준 시안처럼 짝수 행 5개·홀수 행 4개가 교차하도록 배치 좌표 계산 */
    List<WatermarkPlacement> calculatePlacements(int width, int height) {
        int logoWidth = calculateLogoWidth(width);
        int logoHeight = Math.max(1, logoWidth * logo.getHeight() / logo.getWidth());
        int horizontalGap = Math.max(1, (int) Math.round(width * properties.horizontalGapRatio()));
        int verticalGap = Math.max(1, (int) Math.round(height * properties.verticalGapRatio()));
        int horizontalStep = logoWidth + horizontalGap;
        int verticalStep = logoHeight + verticalGap;
        int marginX = Math.max(
                0,
                (int) Math.round(width * properties.horizontalMarginRatio())
        );
        int marginY = Math.max(
                0,
                (int) Math.round(height * properties.verticalMarginRatio())
        );
        List<WatermarkPlacement> placements = new ArrayList<>();

        int row = 0;
        for (int y = marginY; y < height; y += verticalStep) {
            int stagger = row % 2 == 0 ? 0 : horizontalStep / 2;
            for (int x = marginX + stagger;
                 x + logoWidth <= width;
                 x += horizontalStep) {
                placements.add(new WatermarkPlacement(x, y, logoWidth, logoHeight));
            }
            row++;
        }
        return List.copyOf(placements);
    }

    private int calculateLogoWidth(int imageWidth) {
        int ratioWidth = (int) Math.round(imageWidth * properties.logoWidthRatio());
        int configuredWidth = Math.max(
                properties.minimumLogoWidth(),
                Math.min(properties.maximumLogoWidth(), ratioWidth)
        );
        return Math.max(1, Math.min(configuredWidth, imageWidth));
    }

    private void configureQuality(Graphics2D graphics) {
        graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC
        );
        graphics.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY
        );
        graphics.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );
    }

    private byte[] encode(
            BufferedImage image,
            PromptImageContentType contentType
    ) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (contentType == PromptImageContentType.PNG) {
                if (!ImageIO.write(image, "png", output)) {
                    throw new IOException("PNG 인코더를 찾을 수 없습니다.");
                }
            } else {
                writeJpeg(image, output);
            }
            return output.toByteArray();
        } catch (IOException exception) {
            throw new PromptDomainException(
                    PromptErrorCode.IMAGE_WATERMARK_RENDER_FAILED,
                    "워터마크 결과 이미지 인코딩에 실패했습니다.",
                    exception
            );
        }
    }

    private void writeJpeg(BufferedImage image, ByteArrayOutputStream output) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("JPEG 인코더를 찾을 수 없습니다.");
        }

        ImageWriter writer = writers.next();
        try (ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            writeParam.setCompressionQuality((float) properties.jpegQuality());
            writer.write(null, new IIOImage(image, null, null), writeParam);
        } finally {
            writer.dispose();
        }
    }

    private PromptDomainException invalidSource(String message, Throwable cause) {
        return new PromptDomainException(
                PromptErrorCode.INVALID_IMAGE_SOURCE,
                message,
                cause
        );
    }

    record WatermarkPlacement(int x, int y, int width, int height) {
    }
}
