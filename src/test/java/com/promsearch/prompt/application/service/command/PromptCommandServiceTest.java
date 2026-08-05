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
import com.promsearch.prompt.application.port.out.prompt.LoadPromptDraftPort;
import com.promsearch.prompt.application.port.out.prompt.LockPromptDraftPort;
import com.promsearch.prompt.application.port.out.pricing.LoadPromptPricingPort;
import com.promsearch.prompt.application.port.out.prompt.SavePromptDraftPort;
import com.promsearch.prompt.application.port.out.prompt.SavePromptPort;
import com.promsearch.prompt.application.port.out.promptimage.LoadPromptImagePort;
import com.promsearch.prompt.application.port.out.promptimage.SavePromptImagePort;
import com.promsearch.prompt.application.port.out.tag.LoadTagPort;
import com.promsearch.prompt.application.port.out.tag.SaveTagPort;
import com.promsearch.prompt.application.port.out.user.PromoteUserGradePort;
import com.promsearch.prompt.application.usecase.dto.CreatePromptCommand;
import com.promsearch.prompt.application.usecase.dto.CreatePromptCommand.ImageReference;
import com.promsearch.prompt.application.usecase.dto.PromptCommandInfo;
import com.promsearch.prompt.application.usecase.dto.PromptDraftInfo;
import com.promsearch.prompt.application.usecase.dto.SavePromptDraftCommand;
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
import java.util.Optional;
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
    private LoadPromptDraftPort loadPromptDraftPort;
    @Mock
    private SavePromptDraftPort savePromptDraftPort;
    @Mock
    private LockPromptDraftPort lockPromptDraftPort;
    @Mock
    private LoadPromptPricingPort loadPromptPricingPort;
    @Mock
    private PromoteUserGradePort promoteUserGradePort;

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
                loadPromptDraftPort,
                savePromptDraftPort,
                lockPromptDraftPort,
                loadPromptPricingPort,
                promoteUserGradePort
        );
        lenient().when(loadTagPort.batchGetByIds(any())).thenReturn(List.of(
                tag(1L, TagType.JOB, "개발"),
                tag(2L, TagType.TASK, "코드 리뷰"),
                tag(3L, TagType.AI_MODEL, "GPT")
        ));
        lenient().when(savePromptPort.create(any(), any()))
                .thenAnswer(invocation -> persisted(invocation.getArgument(0), 10L));
        lenient().when(savePromptDraftPort.saveOrReplaceDraft(any(), any()))
                .thenAnswer(invocation -> draftInfo(invocation.getArgument(0), 99L));
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
        verify(lockPromptDraftPort).lockByUserId(1L);
        verify(loadPromptPricingPort, never()).getPremiumPricePoint();
        verify(promoteUserGradePort).promoteForPostCreation(1L);
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

    @DisplayName("초안 저장은 사용자별 잠금 안에서 제목만 필수로 DRAFT를 생성하거나 전체 교체한다")
    @Test
    void saveDraftWithOnlyTitle() {
        SavePromptDraftCommand command = draftCommand(List.of());

        PromptCommandInfo info = service.save(command);

        verify(lockPromptDraftPort).lockByUserId(1L);
        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(savePromptDraftPort).saveOrReplaceDraft(promptCaptor.capture(), any());
        Prompt draft = promptCaptor.getValue();
        assertThat(draft.getStatus()).isEqualTo(PromptStatus.DRAFT);
        assertThat(draft.getTitle()).isEqualTo("초안 제목");
        assertThat(draft.getDescription()).isNull();
        assertThat(draft.getOutputType()).isNull();
        assertThat(draft.getContentType()).isNull();
        assertThat(draft.getPromptBody()).isNull();
        assertThat(draft.getPricePoint()).isZero();
        assertThat(info.promptId()).isEqualTo(99L);
        assertThat(info.status()).isEqualTo(PromptStatus.DRAFT);
    }

    @DisplayName("초안 저장은 태그 타입, 이미지 소유권, READY 상태, 중복과 정렬을 검증하고 요청 목록으로 전체 교체한다")
    @Test
    void saveDraftReplacesTagsAndImages() {
        PromptImage retained = readyImage(1L);
        retained.attachToDraft(20L, 1L, 9, false);
        PromptImage added = readyImage(1L);
        PromptImage detached = readyImage(1L);
        detached.attachToDraft(20L, 1L, 0, true);
        SavePromptDraftCommand.ImageReference retainedRef = new SavePromptDraftCommand.ImageReference(
                retained.getPromptImageId().id(),
                1,
                false
        );
        SavePromptDraftCommand.ImageReference addedRef = new SavePromptDraftCommand.ImageReference(
                added.getPromptImageId().id(),
                0,
                true
        );
        when(loadPromptDraftPort.findDraftPromptIdByUserId(1L)).thenReturn(Optional.of(20L));
        org.mockito.Mockito.doAnswer(invocation -> draftInfo(invocation.getArgument(0), 20L))
                .when(savePromptDraftPort)
                .saveOrReplaceDraft(any(), any());
        when(loadPromptImagePort.batchGetByIdsForUpdate(List.of(addedRef.imageId(), retainedRef.imageId()).stream()
                .sorted()
                .toList()))
                .thenReturn(List.of(added, retained));
        when(loadPromptImagePort.listByPromptIdForUpdate(20L)).thenReturn(List.of(detached, retained));

        service.save(draftCommand(List.of(retainedRef, addedRef)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PromptImage>> imagesCaptor = ArgumentCaptor.forClass(List.class);
        verify(savePromptImagePort, org.mockito.Mockito.times(2)).updateAll(imagesCaptor.capture());
        assertThat(imagesCaptor.getAllValues().get(0))
                .extracting(image -> image.getPromptImageId().id())
                .containsExactly(detached.getPromptImageId().id(), retained.getPromptImageId().id());
        assertThat(imagesCaptor.getAllValues().get(1))
                .extracting(image -> image.getPromptImageId().id())
                .containsExactly(retained.getPromptImageId().id(), added.getPromptImageId().id());
        assertThat(detached.getPromptId()).isNull();
        assertThat(retained.getPromptId()).isEqualTo(20L);
        assertThat(retained.getSortOrder()).isEqualTo(1);
        assertThat(added.getPromptId()).isEqualTo(20L);
        assertThat(added.isThumbnail()).isTrue();
    }

    @DisplayName("초안 삭제는 없으면 404로 실패하고 삭제 시 연결 이미지를 detach한다")
    @Test
    void deleteDraftNotFoundOrDetachImages() {
        when(loadPromptDraftPort.findDraftPromptIdByUserId(1L)).thenReturn(Optional.empty());
        assertPromptError(() -> service.delete(1L), PromptErrorCode.PROMPT_NOT_FOUND);

        PromptImage image = readyImage(1L);
        image.attachToDraft(20L, 1L, 0, true);
        when(loadPromptDraftPort.findDraftPromptIdByUserId(1L)).thenReturn(Optional.of(20L));
        when(loadPromptImagePort.listByPromptIdForUpdate(20L)).thenReturn(List.of(image));

        service.delete(1L);

        assertThat(image.getPromptId()).isNull();
        verify(savePromptImagePort).updateAll(List.of(image));
        verify(savePromptDraftPort).deleteDraft(1L);
    }

    @DisplayName("게시 생성은 현재 사용자 초안에 연결된 READY 이미지를 새 ACTIVE 프롬프트로 재사용할 수 있다")
    @Test
    void createCanReuseImagesAttachedToOwnDraft() {
        PromptImage draftImage = readyImage(1L);
        draftImage.attachToDraft(20L, 1L, 0, true);
        PromptImage unusedDraftImage = readyImage(1L);
        unusedDraftImage.attachToDraft(20L, 1L, 1, false);
        ImageReference reference = new ImageReference(draftImage.getPromptImageId().id(), 0, true);
        when(loadPromptDraftPort.findDraftPromptIdByUserId(1L)).thenReturn(Optional.of(20L));
        when(loadPromptImagePort.batchGetByIdsForUpdate(List.of(reference.imageId())))
                .thenReturn(List.of(draftImage));
        when(loadPromptImagePort.listByPromptIdForUpdate(20L)).thenReturn(List.of(draftImage, unusedDraftImage));

        service.create(command(PromptContentType.FREE, List.of(reference)));

        assertThat(draftImage.getPromptId()).isEqualTo(10L);
        assertThat(unusedDraftImage.getPromptId()).isNull();
        verify(savePromptImagePort).updateAll(List.of(draftImage, unusedDraftImage));
        verify(savePromptDraftPort).deleteDraft(1L);
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

    private SavePromptDraftCommand draftCommand(List<SavePromptDraftCommand.ImageReference> images) {
        return new SavePromptDraftCommand(
                1L,
                "초안 제목",
                null,
                null,
                List.of(1L),
                List.of(2L),
                List.of(3L),
                null,
                null,
                null,
                PromptVisibility.PUBLIC,
                images
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
                null,
                List.of(),
                null,
                List.of()
        );
    }

    private PromptDraftInfo draftInfo(Prompt prompt, Long id) {
        return new PromptDraftInfo(
                id,
                prompt.getTitle(),
                prompt.getDescription(),
                prompt.getOutputType(),
                List.of(1L),
                List.of(2L),
                List.of(3L),
                null,
                prompt.getContentType(),
                prompt.getPromptBody(),
                prompt.getVisibility(),
                List.of(),
                prompt.getStatus(),
                prompt.getPricePoint(),
                Instant.parse("2026-07-28T12:00:00Z")
        );
    }

    private void assertPromptError(Runnable action, PromptErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(PromptDomainException.class)
                .extracting("baseCode")
                .isEqualTo(errorCode);
    }
}
