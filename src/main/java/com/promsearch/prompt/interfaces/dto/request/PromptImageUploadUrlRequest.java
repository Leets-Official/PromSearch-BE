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
        @NotEmpty(message = "업로드할 이미지는 한 장 이상이어야 합니다.")
        @Size(max = 10, message = "이미지는 최대 10장까지 업로드할 수 있습니다.")
        List<@Valid ImageFile> images
) {

    @Schema(description = "업로드할 개별 이미지 메타데이터")
    public record ImageFile(
            @Schema(description = "원본 파일명", example = "prompt-result.jpg")
            @NotBlank(message = "파일명은 필수입니다.")
            String fileName,

            @Schema(
                    description = "이미지 MIME 타입",
                    example = "image/jpeg",
                    allowableValues = {"image/jpeg", "image/png"}
            )
            @NotBlank(message = "이미지 MIME 타입은 필수입니다.")
            @Pattern(
                    regexp = "image/(jpeg|png)",
                    message = "이미지 형식은 JPEG 또는 PNG만 지원합니다."
            )
            String contentType,

            @Schema(
                    description = "클라이언트가 선언한 파일 크기(byte). 업로드 완료 후 서버가 다시 검증하며 개별 최대 10MB입니다.",
                    example = "5242880",
                    maximum = "10485760"
            )
            @NotNull(message = "파일 크기는 필수입니다.")
            @Positive(message = "파일 크기는 0보다 커야 합니다.")
            @Max(value = 10_485_760, message = "파일 크기는 10MB 이하여야 합니다.")
            Long fileSize,

            @Schema(description = "클라이언트가 선언한 이미지 가로 픽셀. Worker가 실제 값을 다시 검증합니다.", example = "1920", maximum = "8192")
            @NotNull(message = "이미지 가로 크기는 필수입니다.")
            @Positive(message = "이미지 가로 크기는 0보다 커야 합니다.")
            @Max(value = 8_192, message = "이미지 가로 크기는 8192픽셀 이하여야 합니다.")
            Integer width,

            @Schema(description = "클라이언트가 선언한 이미지 세로 픽셀. Worker가 실제 값을 다시 검증합니다.", example = "1080", maximum = "8192")
            @NotNull(message = "이미지 세로 크기는 필수입니다.")
            @Positive(message = "이미지 세로 크기는 0보다 커야 합니다.")
            @Max(value = 8_192, message = "이미지 세로 크기는 8192픽셀 이하여야 합니다.")
            Integer height
    ) {

        @JsonIgnore
        @Schema(hidden = true)
        @AssertTrue(message = "이미지 전체 픽셀 수는 4천만 이하여야 합니다.")
        public boolean hasValidPixelCount() {
            return width == null || height == null || (long) width * height <= 40_000_000L;
        }
    }
}
