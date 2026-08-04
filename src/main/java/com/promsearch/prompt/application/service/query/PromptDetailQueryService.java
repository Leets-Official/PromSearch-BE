package com.promsearch.prompt.application.service.query;

import com.promsearch.commerce.application.port.out.unlock.CheckPostUnlockPort;
import com.promsearch.prompt.application.port.out.prompt.LoadPromptDetailPort;
import com.promsearch.prompt.application.port.out.prompt.LoadPromptDetailPort.PromptDetailProjection;
import com.promsearch.prompt.application.port.out.storage.PresignPromptImageDownloadPort;
import com.promsearch.prompt.application.usecase.GetPromptDetailUseCase;
import com.promsearch.prompt.application.usecase.dto.PromptDetailInfo;
import com.promsearch.prompt.application.usecase.dto.PromptDetailInfo.Access;
import com.promsearch.prompt.application.usecase.dto.PromptDetailInfo.AccessReason;
import com.promsearch.prompt.application.usecase.dto.PromptDetailInfo.Author;
import com.promsearch.prompt.application.usecase.dto.PromptDetailInfo.Image;
import com.promsearch.prompt.application.usecase.dto.PromptDetailInfo.Statistics;
import com.promsearch.prompt.application.usecase.dto.PromptDetailInfo.Tag;
import com.promsearch.prompt.application.usecase.dto.PromptDetailInfo.ViewerInteraction;
import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromptDetailQueryService implements GetPromptDetailUseCase {

    private static final int PREMIUM_PREVIEW_MAX_LENGTH = 200;

    private final LoadPromptDetailPort loadPromptDetailPort;
    private final CheckPostUnlockPort checkPostUnlockPort;
    private final PresignPromptImageDownloadPort imageStorage;

    @Override
    public PromptDetailInfo get(Long promptId, Long viewerId) {
        if (promptId == null || promptId <= 0) {
            throw new PromptDomainException(PromptErrorCode.INVALID_ID);
        }
        PromptDetailProjection prompt = loadPromptDetailPort.findPublicById(promptId, viewerId)
                .orElseThrow(() -> new PromptDomainException(PromptErrorCode.PROMPT_NOT_FOUND));

        boolean owner = viewerId != null && viewerId.equals(prompt.authorId());
        boolean unlocked = owner || (viewerId != null
                && prompt.contentType() == PromptContentType.PREMIUM
                && checkPostUnlockPort.isUnlocked(viewerId, promptId));
        BodyAccess bodyAccess = resolveBody(prompt, viewerId, owner, unlocked);

        return new PromptDetailInfo(
                prompt.promptId(),
                prompt.title(),
                new Author(
                        prompt.authorId(),
                        prompt.authorNickname(),
                        prompt.authorProfileImageUrl()),
                prompt.outputType(),
                prompt.contentType(),
                prompt.pricePoint(),
                bodyAccess.body(),
                prompt.description(),
                new Access(bodyAccess.locked(), bodyAccess.reason()),
                new ViewerInteraction(prompt.liked(), prompt.bookmarked()),
                prompt.images().stream()
                        .map(image -> new Image(
                                image.imageId(),
                                imageStorage.presignGet(image.watermarkedObjectKey()),
                                image.sortOrder(),
                                image.thumbnail()))
                        .toList(),
                prompt.tags().stream()
                        .map(tag -> new Tag(tag.tagId(), tag.tagType(), tag.name()))
                        .toList(),
                prompt.customAiModels(),
                new Statistics(
                        prompt.statistics().viewCount(),
                        prompt.statistics().copyCount(),
                        prompt.statistics().likeCount(),
                        prompt.statistics().commentCount()),
                prompt.createdAt(),
                prompt.updatedAt()
        );
    }

    private BodyAccess resolveBody(
            PromptDetailProjection prompt,
            Long viewerId,
            boolean owner,
            boolean unlocked
    ) {
        String body = prompt.promptBody() == null ? "" : prompt.promptBody();
        if (viewerId == null) {
            return new BodyAccess("", true, AccessReason.ANONYMOUS);
        }
        if (prompt.contentType() == PromptContentType.FREE) {
            return new BodyAccess(body, false, AccessReason.FREE);
        }
        if (owner) {
            return new BodyAccess(body, false, AccessReason.AUTHOR);
        }
        if (unlocked) {
            return new BodyAccess(body, false, AccessReason.UNLOCKED);
        }
        int bodyCodePointCount = body.codePointCount(0, body.length());
        int previewCodePointCount = Math.min(
                PREMIUM_PREVIEW_MAX_LENGTH,
                bodyCodePointCount / 10
        );
        int previewEndIndex = body.offsetByCodePoints(0, previewCodePointCount);
        return new BodyAccess(body.substring(0, previewEndIndex), true, AccessReason.PREMIUM);
    }

    private record BodyAccess(String body, boolean locked, AccessReason reason) {
    }
}
