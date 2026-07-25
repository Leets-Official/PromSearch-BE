package com.promsearch.prompt.interfaces.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "S3 임시 영역에 직접 업로드할 이미지의 Presigned URL 발급 요청")
public record PromptImageUploadUrlRequest(
        @Schema(description = "업로드할 이미지 메타데이터. 게시물당 최대 10장입니다.")
        @NotEmpty(message = "images must not be empty")
        @Size(max = 10, message = "images must contain 10 items or less")
        List<@Valid ImageFile> images
) {

    @Schema(description = "업로드할 개별 이미지 메타데이터")
    public record ImageFile(
            @Schema(description = "원본 파일명", example = "prompt-result.webp")
            @NotBlank(message = "fileName must not be blank")
            String fileName,

            @Schema(
                    description = "이미지 MIME 타입",
                    example = "image/webp",
                    allowableValues = {"image/jpeg", "image/png", "image/webp"}
            )
            @NotBlank(message = "contentType must not be blank")
            @Pattern(
                    regexp = "image/(jpeg|png|webp)",
                    message = "contentType must be image/jpeg, image/png, or image/webp"
            )
            String contentType,

            @Schema(description = "파일 크기(byte). 개별 최대 10MB입니다.", example = "5242880", maximum = "10485760")
            @NotNull(message = "fileSize must not be null")
            @Positive(message = "fileSize must be greater than 0")
            @Max(value = 10_485_760, message = "fileSize must be 10MB or less")
            Long fileSize,

            @Schema(description = "이미지 가로 픽셀", example = "1920", maximum = "8192")
            @NotNull(message = "width must not be null")
            @Positive(message = "width must be greater than 0")
            @Max(value = 8_192, message = "width must be 8192 pixels or less")
            Integer width,

            @Schema(description = "이미지 세로 픽셀", example = "1080", maximum = "8192")
            @NotNull(message = "height must not be null")
            @Positive(message = "height must be greater than 0")
            @Max(value = 8_192, message = "height must be 8192 pixels or less")
            Integer height
    ) {

        @JsonIgnore
        @Schema(hidden = true)
        @AssertTrue(message = "image pixel count must be 40000000 or less")
        public boolean hasValidPixelCount() {
            return width == null || height == null || (long) width * height <= 40_000_000L;
        }
    }
}
