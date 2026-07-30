package com.promsearch.prompt.application.service.command;

import com.promsearch.prompt.application.port.out.author.LoadPromptAuthorPort;
import com.promsearch.prompt.application.port.out.pricing.LoadPromptPricingPort;
import com.promsearch.prompt.application.port.out.prompt.SavePromptPort;
import com.promsearch.prompt.application.port.out.promptimage.LoadPromptImagePort;
import com.promsearch.prompt.application.port.out.promptimage.SavePromptImagePort;
import com.promsearch.prompt.application.port.out.tag.LoadTagPort;
import com.promsearch.prompt.application.port.out.tag.SaveTagPort;
import com.promsearch.prompt.application.usecase.CreatePromptUseCase;
import com.promsearch.prompt.application.usecase.dto.CreatePromptCommand;
import com.promsearch.prompt.application.usecase.dto.CreatePromptCommand.ImageReference;
import com.promsearch.prompt.application.usecase.dto.PromptCommandInfo;
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
public class PromptCommandService implements CreatePromptUseCase {

    private static final int MAX_IMAGE_COUNT = 10;

    private final LoadPromptAuthorPort loadPromptAuthorPort;
    private final LoadTagPort loadTagPort;
    private final SaveTagPort saveTagPort;
    private final LoadPromptImagePort loadPromptImagePort;
    private final SavePromptImagePort savePromptImagePort;
    private final SavePromptPort savePromptPort;
    private final LoadPromptPricingPort loadPromptPricingPort;

    @Override
    public PromptCommandInfo create(CreatePromptCommand command) {
        validateCommand(command);
        loadPromptAuthorPort.validateActive(command.userId());

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

        List<PromptImage> attachedImages = attachImages(
                command.userId(),
                savedPrompt.getPromptId().id(),
                command.images(),
                imagesById
        );
        savePromptImagePort.updateAll(attachedImages);
        return PromptCommandInfo.from(savedPrompt);
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

    private List<PromptImage> attachImages(
            Long userId,
            Long promptId,
            List<ImageReference> imageReferences,
            Map<UUID, PromptImage> imagesById
    ) {
        List<PromptImage> attachedImages = new ArrayList<>(imageReferences.size());
        for (ImageReference reference : imageReferences) {
            PromptImage image = imagesById.get(reference.imageId());
            if (image == null) {
                throw new PromptDomainException(PromptErrorCode.IMAGE_NOT_FOUND);
            }
            image.attachToPrompt(promptId, userId, reference.sortOrder(), reference.thumbnail());
            attachedImages.add(image);
        }
        return attachedImages;
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
}
