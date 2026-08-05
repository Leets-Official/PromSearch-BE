package com.promsearch.prompt.application.service.command;

import com.promsearch.prompt.application.port.out.author.LoadPromptAuthorPort;
import com.promsearch.prompt.application.port.out.prompt.DeletePromptPort;
import com.promsearch.prompt.application.port.out.prompt.LoadPromptDraftPort;
import com.promsearch.prompt.application.port.out.prompt.LockPromptDraftPort;
import com.promsearch.prompt.application.port.out.pricing.LoadPromptPricingPort;
import com.promsearch.prompt.application.port.out.prompt.SavePromptDraftPort;
import com.promsearch.prompt.application.port.out.prompt.SavePromptPort;
import com.promsearch.prompt.application.port.out.user.PromoteUserGradePort;
import com.promsearch.prompt.application.port.out.promptimage.LoadPromptImagePort;
import com.promsearch.prompt.application.port.out.promptimage.SavePromptImagePort;
import com.promsearch.prompt.application.port.out.tag.LoadTagPort;
import com.promsearch.prompt.application.port.out.tag.SaveTagPort;
import com.promsearch.prompt.application.usecase.CreatePromptUseCase;
import com.promsearch.prompt.application.usecase.DeletePromptDraftUseCase;
import com.promsearch.prompt.application.usecase.DeletePromptUseCase;
import com.promsearch.prompt.application.usecase.SavePromptDraftUseCase;
import com.promsearch.prompt.application.usecase.dto.CreatePromptCommand;
import com.promsearch.prompt.application.usecase.dto.CreatePromptCommand.ImageReference;
import com.promsearch.prompt.application.usecase.dto.PromptCommandInfo;
import com.promsearch.prompt.application.usecase.dto.PromptDraftInfo;
import com.promsearch.prompt.application.usecase.dto.SavePromptDraftCommand;
import com.promsearch.prompt.domain.Prompt;
import com.promsearch.prompt.domain.PromptImage;
import com.promsearch.prompt.domain.Tag;
import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.TagType;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PromptCommandService implements
        CreatePromptUseCase,
        SavePromptDraftUseCase,
        DeletePromptDraftUseCase,
        DeletePromptUseCase {

    private static final int MAX_IMAGE_COUNT = 10;

    private final LoadPromptAuthorPort loadPromptAuthorPort;
    private final LoadTagPort loadTagPort;
    private final SaveTagPort saveTagPort;
    private final LoadPromptImagePort loadPromptImagePort;
    private final SavePromptImagePort savePromptImagePort;
    private final SavePromptPort savePromptPort;
    private final LoadPromptDraftPort loadPromptDraftPort;
    private final SavePromptDraftPort savePromptDraftPort;
    private final LockPromptDraftPort lockPromptDraftPort;
    private final LoadPromptPricingPort loadPromptPricingPort;
    private final PromoteUserGradePort promoteUserGradePort;
    private final DeletePromptPort deletePromptPort;

    @Override
    public PromptCommandInfo create(CreatePromptCommand command) {
        validateCommand(command);
        loadPromptAuthorPort.validateActive(command.userId());
        lockPromptDraftPort.lockByUserId(command.userId());

        long pricePoint = resolvePrice(command.contentType());
        Prompt prompt = Prompt.createActive(
                command.userId(),
                command.title(),
                command.promptBody(),
                command.outputType(),
                command.description(),
                command.contentType(),
                command.visibility(),
                pricePoint
        );
        List<Tag> tags = resolveTags(command);
        Map<UUID, PromptImage> imagesById = loadImagesForUpdate(command.images());
        Prompt savedPrompt = savePromptPort.create(prompt, tags);
        promoteUserGradePort.promoteForPostCreation(command.userId());

        Optional<Long> reusableDraftPromptId = loadPromptDraftPort.findDraftPromptIdByUserId(command.userId());
        List<PromptImage> existingDraftImages = reusableDraftPromptId
                .map(loadPromptImagePort::listByPromptIdForUpdate)
                .orElseGet(List::of);
        List<PromptImage> attachedImages = attachImages(
                command.userId(),
                savedPrompt.getPromptId().id(),
                command.images(),
                imagesById,
                reusableDraftPromptId.orElse(null)
        );
        attachedImages.addAll(detachUnusedDraftImages(
                command.userId(),
                reusableDraftPromptId.orElse(null),
                existingDraftImages,
                command.images()
        ));
        savePromptImagePort.updateAll(attachedImages);
        reusableDraftPromptId.ifPresent(ignored -> savePromptDraftPort.deleteDraft(command.userId()));
        return PromptCommandInfo.from(savedPrompt);
    }

    @Override
    public PromptCommandInfo save(SavePromptDraftCommand command) {
        validateDraftCommand(command);
        loadPromptAuthorPort.validateActive(command.userId());
        lockPromptDraftPort.lockByUserId(command.userId());

        Optional<Long> previousDraftPromptId = loadPromptDraftPort.findDraftPromptIdByUserId(command.userId());
        List<Tag> tags = resolveTags(command);
        Map<UUID, PromptImage> requestedImagesById = loadDraftImagesForUpdate(command.images());

        Prompt draft = Prompt.createDraft(
                command.userId(),
                command.title(),
                command.promptBody(),
                command.outputType(),
                command.description(),
                command.contentType(),
                command.visibility()
        );
        PromptDraftInfo savedDraft = savePromptDraftPort.saveOrReplaceDraft(draft, tags);

        replaceDraftImages(
                command.userId(),
                savedDraft.promptId(),
                previousDraftPromptId.orElse(null),
                command.images(),
                requestedImagesById
        );
        return new PromptCommandInfo(
                savedDraft.promptId(),
                savedDraft.status(),
                savedDraft.visibility(),
                savedDraft.pricePoint(),
                savedDraft.updatedAt()
        );
    }

    @Override
    public void delete(Long userId) {
        validateUserId(userId);
        loadPromptAuthorPort.validateActive(userId);
        lockPromptDraftPort.lockByUserId(userId);

        Long draftPromptId = loadPromptDraftPort.findDraftPromptIdByUserId(userId)
                .orElseThrow(() -> new PromptDomainException(PromptErrorCode.PROMPT_NOT_FOUND));
        List<PromptImage> detachedImages = loadPromptImagePort.listByPromptIdForUpdate(draftPromptId);
        for (PromptImage image : detachedImages) {
            image.detachFromPrompt(userId, draftPromptId);
        }
        savePromptImagePort.updateAll(detachedImages);
        savePromptDraftPort.deleteDraft(userId);
    }

    @Override
    public void delete(Long promptId, Long userId) {
        validateUserId(userId);
        if (promptId == null || promptId <= 0) {
            throw new PromptDomainException(PromptErrorCode.INVALID_ID);
        }
        deletePromptPort.delete(promptId, userId);
    }

    private List<Tag> resolveTags(CreatePromptCommand command) {
        Set<Long> jobTagIds = normalizeTagIds(command.jobTagIds());
        Set<Long> taskTagIds = normalizeTagIds(command.taskTagIds());
        Set<Long> aiModelTagIds = normalizeTagIds(command.aiModelTagIds());

        Set<Long> allTagIds = new LinkedHashSet<>();
        allTagIds.addAll(jobTagIds);
        allTagIds.addAll(taskTagIds);
        allTagIds.addAll(aiModelTagIds);

        Map<Long, Tag> tagsById = loadTagPort.batchGetByIds(allTagIds).stream()
                .collect(Collectors.toMap(
                        tag -> tag.getTagId().id(),
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));
        validateTagTypes(tagsById, jobTagIds, TagType.JOB);
        validateTagTypes(tagsById, taskTagIds, TagType.TASK);
        validateTagTypes(tagsById, aiModelTagIds, TagType.AI_MODEL);

        if (command.customAiModel() != null && !command.customAiModel().isBlank()) {
            Tag customAiModel = saveTagPort.getOrCreateCustomAiModel(
                    Tag.createCustomAiModel(command.customAiModel())
            );
            tagsById.put(customAiModel.getTagId().id(), customAiModel);
        }
        return List.copyOf(tagsById.values());
    }

    private List<Tag> resolveTags(SavePromptDraftCommand command) {
        Set<Long> jobTagIds = normalizeTagIds(command.jobTagIds());
        Set<Long> taskTagIds = normalizeTagIds(command.taskTagIds());
        Set<Long> aiModelTagIds = normalizeTagIds(command.aiModelTagIds());

        Set<Long> allTagIds = new LinkedHashSet<>();
        allTagIds.addAll(jobTagIds);
        allTagIds.addAll(taskTagIds);
        allTagIds.addAll(aiModelTagIds);

        Map<Long, Tag> tagsById = loadTagPort.batchGetByIds(allTagIds).stream()
                .collect(Collectors.toMap(
                        tag -> tag.getTagId().id(),
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));
        validateTagTypes(tagsById, jobTagIds, TagType.JOB);
        validateTagTypes(tagsById, taskTagIds, TagType.TASK);
        validateTagTypes(tagsById, aiModelTagIds, TagType.AI_MODEL);

        if (command.customAiModel() != null && !command.customAiModel().isBlank()) {
            Tag customAiModel = saveTagPort.getOrCreateCustomAiModel(
                    Tag.createCustomAiModel(command.customAiModel())
            );
            tagsById.put(customAiModel.getTagId().id(), customAiModel);
        }
        return List.copyOf(tagsById.values());
    }

    private Set<Long> normalizeTagIds(List<Long> tagIds) {
        if (tagIds.stream().anyMatch(tagId -> tagId == null || tagId <= 0)) {
            throw new PromptDomainException(PromptErrorCode.INVALID_TAG_ID);
        }
        return new LinkedHashSet<>(tagIds);
    }

    private void validateTagTypes(Map<Long, Tag> tagsById, Collection<Long> tagIds, TagType expectedType) {
        for (Long tagId : tagIds) {
            Tag tag = tagsById.get(tagId);
            if (tag == null) {
                throw new PromptDomainException(PromptErrorCode.TAG_NOT_FOUND);
            }
            if (tag.getTagType() != expectedType) {
                throw new PromptDomainException(PromptErrorCode.INVALID_TAG_TYPE);
            }
        }
    }

    private Map<UUID, PromptImage> loadImagesForUpdate(List<ImageReference> imageReferences) {
        if (imageReferences.isEmpty()) {
            return Map.of();
        }
        List<UUID> imageIds = imageReferences.stream()
                .map(ImageReference::imageId)
                .sorted()
                .toList();
        return loadPromptImagePort.batchGetByIdsForUpdate(imageIds).stream()
                .collect(Collectors.toMap(
                        image -> image.getPromptImageId().id(),
                        Function.identity()
                ));
    }

    private Map<UUID, PromptImage> loadDraftImagesForUpdate(List<SavePromptDraftCommand.ImageReference> imageReferences) {
        if (imageReferences.isEmpty()) {
            return Map.of();
        }
        List<UUID> imageIds = imageReferences.stream()
                .map(SavePromptDraftCommand.ImageReference::imageId)
                .sorted()
                .toList();
        return loadPromptImagePort.batchGetByIdsForUpdate(imageIds).stream()
                .collect(Collectors.toMap(
                        image -> image.getPromptImageId().id(),
                        Function.identity()
                ));
    }

    private List<PromptImage> attachImages(
            Long userId,
            Long promptId,
            List<ImageReference> imageReferences,
            Map<UUID, PromptImage> imagesById,
            Long reusableDraftPromptId
    ) {
        List<PromptImage> attachedImages = new ArrayList<>(imageReferences.size());
        for (ImageReference reference : imageReferences) {
            PromptImage image = imagesById.get(reference.imageId());
            if (image == null) {
                throw new PromptDomainException(PromptErrorCode.IMAGE_NOT_FOUND);
            }
            image.attachToPrompt(promptId, userId, reference.sortOrder(), reference.thumbnail(), reusableDraftPromptId);
            attachedImages.add(image);
        }
        return attachedImages;
    }

    private void replaceDraftImages(
            Long userId,
            Long draftPromptId,
            Long previousDraftPromptId,
            List<SavePromptDraftCommand.ImageReference> imageReferences,
            Map<UUID, PromptImage> requestedImagesById
    ) {
        List<PromptImage> existingDraftImages = List.of();
        if (previousDraftPromptId != null) {
            existingDraftImages = loadPromptImagePort.listByPromptIdForUpdate(previousDraftPromptId);
            for (PromptImage image : existingDraftImages) {
                image.detachFromPrompt(userId, previousDraftPromptId);
            }
            // (prompt_id, sort_order) unique 제약과 충돌하지 않도록 기존 배치를 먼저 비우고 flush한다.
            savePromptImagePort.updateAll(existingDraftImages);
        }

        List<PromptImage> attachedImages = new ArrayList<>(imageReferences.size());
        for (SavePromptDraftCommand.ImageReference reference : imageReferences) {
            PromptImage image = requestedImagesById.get(reference.imageId());
            if (image == null) {
                throw new PromptDomainException(PromptErrorCode.IMAGE_NOT_FOUND);
            }
            image.attachToDraft(draftPromptId, userId, reference.sortOrder(), reference.thumbnail());
            attachedImages.add(image);
        }
        savePromptImagePort.updateAll(attachedImages);
    }

    private List<PromptImage> detachUnusedDraftImages(
            Long userId,
            Long draftPromptId,
            List<PromptImage> existingDraftImages,
            List<ImageReference> publishedImageReferences
    ) {
        if (draftPromptId == null || existingDraftImages.isEmpty()) {
            return List.of();
        }

        Set<UUID> publishedImageIds = publishedImageReferences.stream()
                .map(ImageReference::imageId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<PromptImage> detachedImages = new ArrayList<>();
        for (PromptImage image : existingDraftImages) {
            if (!publishedImageIds.contains(image.getPromptImageId().id())) {
                image.detachFromPrompt(userId, draftPromptId);
                detachedImages.add(image);
            }
        }
        return detachedImages;
    }

    private long resolvePrice(PromptContentType contentType) {
        return contentType == PromptContentType.FREE
                ? 0L
                : loadPromptPricingPort.getPremiumPricePoint();
    }

    private void validateCommand(CreatePromptCommand command) {
        if (command == null) {
            throw new PromptDomainException(PromptErrorCode.INVALID_ID);
        }
        if (command.userId() == null || command.userId() <= 0) {
            throw new PromptDomainException(PromptErrorCode.INVALID_PROMPT_USER_ID);
        }
        if (command.jobTagIds().isEmpty() || command.taskTagIds().isEmpty()) {
            throw new PromptDomainException(PromptErrorCode.REQUIRED_TAG_MISSING);
        }
        if (command.aiModelTagIds().isEmpty()
                && (command.customAiModel() == null || command.customAiModel().isBlank())) {
            throw new PromptDomainException(PromptErrorCode.REQUIRED_TAG_MISSING);
        }
        if (command.images().isEmpty()) {
            throw new PromptDomainException(PromptErrorCode.IMAGE_REQUIRED);
        }
        if (command.images().size() > MAX_IMAGE_COUNT || command.images().stream().anyMatch(Objects::isNull)) {
            throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_METADATA);
        }

        Set<UUID> imageIds = new LinkedHashSet<>();
        Set<Integer> sortOrders = new LinkedHashSet<>();
        int thumbnailCount = 0;
        for (ImageReference image : command.images()) {
            if (image.imageId() == null) {
                throw new PromptDomainException(PromptErrorCode.INVALID_ID);
            }
            if (!imageIds.add(image.imageId())) {
                throw new PromptDomainException(PromptErrorCode.DUPLICATE_IMAGE);
            }
            if (image.sortOrder() < 0) {
                throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_ORDER);
            }
            if (!sortOrders.add(image.sortOrder())) {
                throw new PromptDomainException(PromptErrorCode.DUPLICATE_IMAGE_ORDER);
            }
            if (image.thumbnail() && ++thumbnailCount > 1) {
                throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_METADATA);
            }
        }
    }

    private void validateDraftCommand(SavePromptDraftCommand command) {
        if (command == null) {
            throw new PromptDomainException(PromptErrorCode.INVALID_ID);
        }
        validateUserId(command.userId());
        if (command.title() == null
                || command.title().isBlank()
                || command.title().strip().length() > Prompt.MAX_TITLE_LENGTH) {
            throw new PromptDomainException(PromptErrorCode.INVALID_PROMPT_TITLE);
        }
        if (command.visibility() == null) {
            throw new PromptDomainException(PromptErrorCode.INVALID_PROMPT_VISIBILITY);
        }
        if (command.images().size() > MAX_IMAGE_COUNT || command.images().stream().anyMatch(Objects::isNull)) {
            throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_METADATA);
        }

        Set<UUID> imageIds = new LinkedHashSet<>();
        Set<Integer> sortOrders = new LinkedHashSet<>();
        int thumbnailCount = 0;
        for (SavePromptDraftCommand.ImageReference image : command.images()) {
            if (image.imageId() == null) {
                throw new PromptDomainException(PromptErrorCode.INVALID_ID);
            }
            if (!imageIds.add(image.imageId())) {
                throw new PromptDomainException(PromptErrorCode.DUPLICATE_IMAGE);
            }
            if (image.sortOrder() < 0) {
                throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_ORDER);
            }
            if (!sortOrders.add(image.sortOrder())) {
                throw new PromptDomainException(PromptErrorCode.DUPLICATE_IMAGE_ORDER);
            }
            if (image.thumbnail() && ++thumbnailCount > 1) {
                throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_METADATA);
            }
        }
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new PromptDomainException(PromptErrorCode.INVALID_PROMPT_USER_ID);
        }
    }
}
