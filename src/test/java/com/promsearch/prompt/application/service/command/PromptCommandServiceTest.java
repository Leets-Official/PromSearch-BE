package com.promsearch.prompt.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.promsearch.prompt.application.port.out.author.LoadPromptAuthorPort;
import com.promsearch.prompt.application.port.out.pricing.LoadPromptPricingPort;
import com.promsearch.prompt.application.port.out.prompt.SavePromptPort;
import com.promsearch.prompt.application.port.out.promptimage.LoadPromptImagePort;
import com.promsearch.prompt.application.port.out.promptimage.SavePromptImagePort;
import com.promsearch.prompt.application.port.out.tag.LoadTagPort;
import com.promsearch.prompt.application.port.out.tag.SaveTagPort;
import com.promsearch.prompt.application.usecase.dto.CreatePromptCommand;
import com.promsearch.prompt.application.usecase.dto.CreatePromptCommand.ImageReference;
import com.promsearch.prompt.application.usecase.dto.PromptCommandInfo;
import com.promsearch.prompt.domain.Prompt;
import com.promsearch.prompt.domain.Prompt.PromptId;
import com.promsearch.prompt.domain.PromptImage;
import com.promsearch.prompt.domain.Tag;
import com.promsearch.prompt.domain.Tag.TagId;
import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptImageContentType;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.domain.enums.PromptVisibility;
import com.promsearch.prompt.domain.enums.TagType;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PromptCommandServiceTest {

    @Mock
    private LoadPromptAuthorPort loadPromptAuthorPort;
    @Mock
    private LoadTagPort loadTagPort;
    @Mock
    private SaveTagPort saveTagPort;
    @Mock
    private LoadPromptImagePort loadPromptImagePort;
    @Mock
    private SavePromptImagePort savePromptImagePort;
    @Mock
    private SavePromptPort savePromptPort;
    @Mock
    private LoadPromptPricingPort loadPromptPricingPort;

    private PromptCommandService service;

    @BeforeEach
    void setUp() {
        service = new PromptCommandService(
                loadPromptAuthorPort,
                loadTagPort,
                saveTagPort,
                loadPromptImagePort,
                savePromptImagePort,
                savePromptPort,
                loadPromptPricingPort
        );
        lenient().when(loadTagPort.batchGetByIds(any())).thenReturn(List.of(
                tag(1L, TagType.JOB, "개발"),
                tag(2L, TagType.TASK, "코드 리뷰"),
                tag(3L, TagType.AI_MODEL, "GPT")
        ));
        lenient().when(savePromptPort.create(any(), any()))
                .thenAnswer(invocation -> persisted(invocation.getArgument(0), 10L));
    }

    @DisplayName("FREE 프롬프트와 초기 연관 데이터를 생성한다")
    @Test
    void createFreePrompt() {
        PromptCommandInfo info = service.create(command(PromptContentType.FREE, List.of()));

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(savePromptPort).create(promptCaptor.capture(), any());
        Prompt saved = promptCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(PromptStatus.ACTIVE);
        assertThat(saved.getVisibility()).isEqualTo(PromptVisibility.PUBLIC);
        assertThat(saved.getPricePoint()).isZero();
        assertThat(info.promptId()).isEqualTo(10L);
        assertThat(info.status()).isEqualTo(PromptStatus.ACTIVE);
        verify(loadPromptPricingPort, never()).getPremiumPricePoint();
    }

    @DisplayName("PREMIUM 가격은 서버 설정 포트에서 결정한다")
    @Test
    void premiumUsesConfiguredPrice() {
        when(loadPromptPricingPort.getPremiumPricePoint()).thenReturn(250L);

        service.create(command(PromptContentType.PREMIUM, List.of()));

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(savePromptPort).create(promptCaptor.capture(), any());
        assertThat(promptCaptor.getValue().getPricePoint()).isEqualTo(250L);
    }

    @DisplayName("비활성 또는 존재하지 않는 사용자는 프롬프트를 생성할 수 없다")
    @Test
    void rejectInactiveUser() {
        doThrow(new UserDomainException(UserErrorCode.USER_NOT_FOUND))
                .when(loadPromptAuthorPort)
                .validateActive(1L);

        assertThatThrownBy(() -> service.create(command(PromptContentType.FREE, List.of())))
                .isInstanceOf(UserDomainException.class)
                .extracting("baseCode")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
        verify(savePromptPort, never()).create(any(), any());
    }

    @DisplayName("AI 모델 ID 없이 custom AI 모델만 입력해도 필수 선택으로 인정한다")
    @Test
    void resolveTypedAndCustomTags() {
        Tag job = tag(1L, TagType.JOB, "개발");
        Tag task = tag(2L, TagType.TASK, "코드 리뷰");
        when(loadTagPort.batchGetByIds(any())).thenReturn(List.of(job, task));
        when(saveTagPort.getOrCreateCustomAiModel(any()))
                .thenReturn(Tag.reconstruct(
                        new TagId(4L),
                        TagType.AI_MODEL,
                        "GPT 4.1 Mini",
                        "gpt4.1mini",
                        true
                ));

        service.create(new CreatePromptCommand(
                1L,
                "제목",
                "설명",
                PromptOutputType.TEXT,
                List.of(1L, 1L),
                List.of(2L),
                List.of(),
                "GPT 4.1 Mini",
                PromptContentType.FREE,
                "본문",
                PromptVisibility.PUBLIC,
                command(PromptContentType.FREE, List.of()).images()
        ));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Tag>> tagsCaptor = ArgumentCaptor.forClass(List.class);
        verify(savePromptPort).create(any(), tagsCaptor.capture());
        assertThat(tagsCaptor.getValue())
                .extracting(tag -> tag.getTagId().id())
                .containsExactly(1L, 2L, 4L);
        ArgumentCaptor<Tag> customTagCaptor = ArgumentCaptor.forClass(Tag.class);
        verify(saveTagPort).getOrCreateCustomAiModel(customTagCaptor.capture());
        assertThat(customTagCaptor.getValue().getNormalizedName()).isEqualTo("gpt4.1mini");
    }

    @DisplayName("본인 소유 READY 이미지를 요청 순서와 썸네일 정보로 연결한다")
    @Test
    void attachReadyImages() {
        PromptImage firstImage = readyImage(1L);
        PromptImage secondImage = readyImage(1L);
        ImageReference firstReference = new ImageReference(
                firstImage.getPromptImageId().id(),
                2,
                true
        );
        ImageReference secondReference = new ImageReference(
                secondImage.getPromptImageId().id(),
                1,
                false
        );
        List<UUID> sortedImageIds = List.of(firstReference.imageId(), secondReference.imageId()).stream()
                .sorted()
                .toList();
        when(loadPromptImagePort.batchGetByIdsForUpdate(sortedImageIds))
                .thenReturn(List.of(firstImage, secondImage));

        service.create(command(PromptContentType.FREE, List.of(firstReference, secondReference)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PromptImage>> imagesCaptor = ArgumentCaptor.forClass(List.class);
        verify(savePromptImagePort).updateAll(imagesCaptor.capture());
        PromptImage attached = imagesCaptor.getValue().getFirst();
        assertThat(attached.getPromptId()).isEqualTo(10L);
        assertThat(attached.getSortOrder()).isEqualTo(2);
        assertThat(attached.isThumbnail()).isTrue();
    }

    @DisplayName("태그 타입이 요청 그룹과 다르면 생성하지 않는다")
    @Test
    void rejectMismatchedTagType() {
        when(loadTagPort.batchGetByIds(any()))
                .thenReturn(List.of(tag(1L, TagType.TASK, "잘못된 타입")));

        CreatePromptCommand command = new CreatePromptCommand(
                1L,
                "제목",
                "설명",
                PromptOutputType.TEXT,
                List.of(1L),
                List.of(2L),
                List.of(3L),
                null,
                PromptContentType.FREE,
                "본문",
                PromptVisibility.PUBLIC,
                command(PromptContentType.FREE, List.of()).images()
        );

        assertPromptError(() -> service.create(command), PromptErrorCode.INVALID_TAG_TYPE);
        verify(savePromptPort, never()).create(any(), any());
    }

    @DisplayName("직군·태스크·AI 모델 태그와 이미지는 생성 시 모두 필요하다")
    @Test
    void rejectMissingRequiredRelations() {
        CreatePromptCommand valid = command(PromptContentType.FREE, List.of());

        CreatePromptCommand missingJobTag = new CreatePromptCommand(
                valid.userId(),
                valid.title(),
                valid.description(),
                valid.outputType(),
                List.of(),
                valid.taskTagIds(),
                valid.aiModelTagIds(),
                valid.customAiModel(),
                valid.contentType(),
                valid.promptBody(),
                valid.visibility(),
                valid.images()
        );
        assertPromptError(() -> service.create(missingJobTag), PromptErrorCode.REQUIRED_TAG_MISSING);

        CreatePromptCommand missingAiModel = new CreatePromptCommand(
                valid.userId(),
                valid.title(),
                valid.description(),
                valid.outputType(),
                valid.jobTagIds(),
                valid.taskTagIds(),
                List.of(),
                null,
                valid.contentType(),
                valid.promptBody(),
                valid.visibility(),
                valid.images()
        );
        assertPromptError(() -> service.create(missingAiModel), PromptErrorCode.REQUIRED_TAG_MISSING);

        CreatePromptCommand missingImage = new CreatePromptCommand(
                valid.userId(),
                valid.title(),
                valid.description(),
                valid.outputType(),
                valid.jobTagIds(),
                valid.taskTagIds(),
                valid.aiModelTagIds(),
                valid.customAiModel(),
                valid.contentType(),
                valid.promptBody(),
                valid.visibility(),
                List.of()
        );
        assertPromptError(() -> service.create(missingImage), PromptErrorCode.IMAGE_REQUIRED);
        verify(savePromptPort, never()).create(any(), any());
    }

    @DisplayName("동일 이미지 또는 정렬 순서 중복은 영속화 전에 거절한다")
    @Test
    void rejectDuplicateImageReferences() {
        UUID imageId = UUID.randomUUID();
        CreatePromptCommand duplicateImage = command(
                PromptContentType.FREE,
                List.of(
                        new ImageReference(imageId, 0, false),
                        new ImageReference(imageId, 1, false)
                )
        );
        assertPromptError(() -> service.create(duplicateImage), PromptErrorCode.DUPLICATE_IMAGE);

        CreatePromptCommand duplicateOrder = command(
                PromptContentType.FREE,
                List.of(
                        new ImageReference(UUID.randomUUID(), 0, false),
                        new ImageReference(UUID.randomUUID(), 0, false)
                )
        );
        assertPromptError(() -> service.create(duplicateOrder), PromptErrorCode.DUPLICATE_IMAGE_ORDER);
        verify(savePromptPort, never()).create(any(), any());
    }

    private CreatePromptCommand command(PromptContentType contentType, List<ImageReference> images) {
        List<ImageReference> requiredImages = images;
        if (requiredImages.isEmpty()) {
            PromptImage image = readyImage(1L);
            ImageReference reference = new ImageReference(
                    image.getPromptImageId().id(),
                    0,
                    true
            );
            requiredImages = List.of(reference);
            lenient().when(loadPromptImagePort.batchGetByIdsForUpdate(List.of(reference.imageId())))
                    .thenReturn(List.of(image));
        }
        return new CreatePromptCommand(
                1L,
                "제목",
                "설명",
                PromptOutputType.TEXT,
                List.of(1L),
                List.of(2L),
                List.of(3L),
                null,
                contentType,
                "본문",
                PromptVisibility.PUBLIC,
                requiredImages
        );
    }

    private Tag tag(Long id, TagType type, String name) {
        return Tag.reconstruct(new TagId(id), type, name, name, false);
    }

    private PromptImage readyImage(Long uploaderId) {
        UUID imageId = UUID.randomUUID();
        PromptImage image = PromptImage.prepareUpload(
                imageId,
                uploaderId,
                "originals/" + uploaderId + "/" + imageId + ".jpg",
                "result.jpg",
                PromptImageContentType.JPEG,
                1_024,
                1_920,
                1_080
        );
        image.completeUpload("\"etag\"", Instant.parse("2026-07-28T01:00:00Z"));
        image.startProcessing();
        image.completeProcessing("watermarked/" + uploaderId + "/" + imageId + ".jpg", 1);
        return image;
    }

    private Prompt persisted(Prompt prompt, Long id) {
        Instant now = Instant.parse("2026-07-28T12:00:00Z");
        return Prompt.reconstruct(
                new PromptId(id),
                prompt.getUserId(),
                prompt.getTitle(),
                prompt.getPromptBody(),
                null,
                prompt.getOutputType(),
                prompt.getDescription(),
                prompt.getContentType(),
                prompt.getStatus(),
                prompt.getVisibility(),
                prompt.getPricePoint(),
                null,
                null,
                now,
                now,
                null,
                List.of(),
                null,
                List.of()
        );
    }

    private void assertPromptError(Runnable action, PromptErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(PromptDomainException.class)
                .extracting("baseCode")
                .isEqualTo(errorCode);
    }
}
