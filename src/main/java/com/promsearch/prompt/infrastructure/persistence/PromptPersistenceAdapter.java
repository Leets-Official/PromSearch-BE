package com.promsearch.prompt.infrastructure.persistence;

import com.promsearch.prompt.application.port.out.prompt.LoadPromptDraftPort;
import com.promsearch.prompt.application.port.out.prompt.LoadPromptEditPort;
import com.promsearch.prompt.application.port.out.prompt.LoadPromptEditPort.ImageProjection;
import com.promsearch.prompt.application.port.out.prompt.LoadPromptEditPort.PromptEditProjection;
import com.promsearch.prompt.application.port.out.prompt.LoadPromptPort;
import com.promsearch.prompt.application.port.out.prompt.MyPromptSummaryRow;
import com.promsearch.prompt.application.port.out.prompt.PromptInsightTotals;
import com.promsearch.prompt.application.port.out.prompt.PromptPageResult;
import com.promsearch.prompt.application.port.out.prompt.SavePromptDraftPort;
import com.promsearch.prompt.application.port.out.prompt.SavePromptPort;
import com.promsearch.prompt.application.usecase.dto.PromptDraftInfo;
import com.promsearch.prompt.application.usecase.dto.PromptDraftInfo.ImageInfo;
import com.promsearch.prompt.domain.Prompt;
import com.promsearch.prompt.domain.Tag;
import com.promsearch.prompt.domain.enums.PromptStatus;
import com.promsearch.prompt.domain.enums.TagType;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import com.promsearch.prompt.infrastructure.persistence.entity.PostJpaEntity;
import com.promsearch.prompt.infrastructure.persistence.entity.PostTagJpaEntity;
import com.promsearch.prompt.infrastructure.persistence.entity.PostStatisticsJpaEntity;
import com.promsearch.prompt.infrastructure.persistence.entity.TagJpaEntity;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PromptPersistenceAdapter implements
        SavePromptPort,
        LoadPromptDraftPort,
        LoadPromptEditPort,
        SavePromptDraftPort,
        LoadPromptPort {

    private final PostRepository postRepository;
    private final TagRepository tagRepository;
    private final PromptImageRepository promptImageRepository;

    @Override
    public PromptPageResult listByUserIdAndStatus(Long userId, PromptStatus status, int page, int size) {
        Page<MyPromptSummaryProjection> result = postRepository.findMyPromptSummaries(
                userId,
                status,
                PageRequest.of(page, size)
        );

        List<MyPromptSummaryRow> content = result.getContent().stream()
                .map(row -> new MyPromptSummaryRow(
                        row.getPromptId(),
                        row.getTitle(),
                        row.getPublishedAt(),
                        row.getViewCount(),
                        row.getRecommendCount()
                ))
                .toList();

        return new PromptPageResult(content, result.getTotalElements());
    }

    @Override
    public PromptInsightTotals sumInsightsByUserId(Long userId) {
        PromptInsightProjection projection = postRepository.sumInsightsByUserId(userId);

        return new PromptInsightTotals(
                projection.getTotalViews(),
                projection.getTotalRecommends(),
                projection.getTotalCopies()
        );
    }

    @Override
    public Prompt create(Prompt prompt, List<Tag> tags) {
        PostJpaEntity post = postRepository.saveAndFlush(PostJpaEntity.from(prompt));

        List<Long> tagIds = tags.stream()
                .map(tag -> tag.getTagId().id())
                .toList();
        List<TagJpaEntity> tagEntities = tagRepository.findAllById(tagIds);
        if (tagEntities.size() != tagIds.size()) {
            throw new PromptDomainException(PromptErrorCode.TAG_NOT_FOUND);
        }

        for (TagJpaEntity tag : tagEntities) {
            post.addPostTag(PostTagJpaEntity.create(post, tag));
        }
        post.initializeStatistics(PostStatisticsJpaEntity.create(post));
        postRepository.flush();
        return post.toDomain();
    }

    @Override
    public Optional<Long> findDraftPromptIdByUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new PromptDomainException(PromptErrorCode.INVALID_PROMPT_USER_ID);
        }
        return postRepository.findDraftIdByUserId(userId);
    }

    @Override
    public Optional<PromptDraftInfo> findDraftByUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new PromptDomainException(PromptErrorCode.INVALID_PROMPT_USER_ID);
        }
        return postRepository.findDraftByUserId(userId)
                .map(this::toDraftInfo);
    }

    @Override
    public Optional<PromptEditProjection> findById(Long promptId) {
        return postRepository.findForEditById(promptId)
                .map(this::toEditProjection);
    }

    @Override
    public PromptDraftInfo saveOrReplaceDraft(Prompt draft, List<Tag> tags) {
        PostJpaEntity post = postRepository.findDraftByUserIdForUpdate(draft.getUserId())
                .orElseGet(() -> postRepository.saveAndFlush(PostJpaEntity.from(draft)));

        List<TagJpaEntity> tagEntities = loadTagEntities(tags);
        post.replaceDraft(draft, tagEntities);
        postRepository.flush();
        return toDraftInfo(post);
    }

    @Override
    public void deleteDraft(Long userId) {
        PostJpaEntity post = postRepository.findDraftByUserIdForUpdate(userId)
                .orElseThrow(() -> new PromptDomainException(PromptErrorCode.PROMPT_NOT_FOUND));
        post.deleteDraft();
        postRepository.flush();
    }

    private List<TagJpaEntity> loadTagEntities(List<Tag> tags) {
        List<Long> tagIds = tags.stream()
                .map(tag -> tag.getTagId().id())
                .toList();
        List<TagJpaEntity> tagEntities = tagRepository.findAllById(tagIds);
        if (tagEntities.size() != tagIds.size()) {
            throw new PromptDomainException(PromptErrorCode.TAG_NOT_FOUND);
        }
        return tagEntities;
    }

    private PromptDraftInfo toDraftInfo(PostJpaEntity post) {
        List<Long> jobTagIds = new ArrayList<>();
        List<Long> taskTagIds = new ArrayList<>();
        List<Long> aiModelTagIds = new ArrayList<>();
        String customAiModel = null;
        Set<Long> seenTagIds = new LinkedHashSet<>();

        for (PostTagJpaEntity postTag : post.getPostTags()) {
            TagJpaEntity tag = postTag.getTag();
            if (!seenTagIds.add(tag.getId())) {
                continue;
            }
            if (tag.getTagType() == TagType.JOB) {
                jobTagIds.add(tag.getId());
            } else if (tag.getTagType() == TagType.TASK) {
                taskTagIds.add(tag.getId());
            } else if (tag.getTagType() == TagType.AI_MODEL && Boolean.TRUE.equals(tag.getCustom())) {
                customAiModel = tag.getTagName();
            } else if (tag.getTagType() == TagType.AI_MODEL) {
                aiModelTagIds.add(tag.getId());
            }
        }

        List<ImageInfo> images = promptImageRepository.findAllByPromptIdOrderBySortOrderAsc(post.getId()).stream()
                .map(image -> new ImageInfo(
                        image.getId(),
                        image.getSortOrder(),
                        image.getThumbnail()
                ))
                .toList();

        return new PromptDraftInfo(
                post.getId(),
                post.getTitle(),
                post.getDescription(),
                post.getOutputType(),
                jobTagIds,
                taskTagIds,
                aiModelTagIds,
                customAiModel,
                post.getContentType(),
                post.getPromptBody(),
                post.getVisibility(),
                images,
                post.getStatus(),
                post.getPricePoint(),
                post.getUpdatedAt()
        );
    }

    private PromptEditProjection toEditProjection(PostJpaEntity post) {
        PromptDraftInfo draft = toDraftInfo(post);
        List<ImageProjection> images = promptImageRepository.findAllByPromptIdOrderBySortOrderAsc(post.getId()).stream()
                .filter(image -> image.getWatermarkedObjectKey() != null && !image.getWatermarkedObjectKey().isBlank())
                .map(image -> new ImageProjection(
                        image.getId(), image.getWatermarkedObjectKey(), image.getSortOrder(), image.getThumbnail()))
                .toList();
        return new PromptEditProjection(
                post.getId(), post.getUserId(), draft.title(), draft.description(), draft.outputType(),
                draft.jobTagIds(), draft.taskTagIds(), draft.aiModelTagIds(), draft.customAiModel(),
                draft.contentType(), draft.promptBody(), draft.visibility(), images, draft.status(),
                draft.pricePoint(), draft.updatedAt());
    }
}
