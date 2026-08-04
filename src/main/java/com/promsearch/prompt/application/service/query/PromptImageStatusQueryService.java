package com.promsearch.prompt.application.service.query;

import com.promsearch.prompt.application.port.out.promptimage.LoadPromptImagePort;
import com.promsearch.prompt.application.port.out.storage.PresignPromptImageDownloadPort;
import com.promsearch.prompt.application.usecase.GetPromptImageStatusesUseCase;
import com.promsearch.prompt.application.usecase.dto.GetPromptImageStatusesQuery;
import com.promsearch.prompt.application.usecase.dto.PromptImageStatusInfo;
import com.promsearch.prompt.application.usecase.dto.PromptImageStatusesInfo;
import com.promsearch.prompt.domain.PromptImage;
import com.promsearch.prompt.domain.exception.PromptDomainException;
import com.promsearch.prompt.domain.exception.PromptErrorCode;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 인증 사용자 이미지 처리 상태 read-only 조회 서비스 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromptImageStatusQueryService implements GetPromptImageStatusesUseCase {

    private static final int MAX_IMAGE_COUNT = 10;

    private final LoadPromptImagePort loadPromptImagePort;
    private final PresignPromptImageDownloadPort presignPromptImageDownloadPort;

    @Override
    public PromptImageStatusesInfo getStatuses(GetPromptImageStatusesQuery query) {
        validateQuery(query);

        List<PromptImage> images = loadPromptImagePort.listByIds(query.imageIds());
        if (images.size() != query.imageIds().size()) {
            throw new PromptDomainException(PromptErrorCode.IMAGE_NOT_FOUND);
        }
        if (images.stream().anyMatch(image -> !image.isOwnedBy(query.requesterId()))) {
            throw new PromptDomainException(PromptErrorCode.IMAGE_NOT_OWNED);
        }

        Map<UUID, PromptImage> imageMap = images.stream()
                .collect(Collectors.toMap(image -> image.getPromptImageId().id(), Function.identity()));
        return new PromptImageStatusesInfo(query.imageIds().stream()
                .map(imageId -> toInfo(imageMap.get(imageId)))
                .toList());
    }

    private PromptImageStatusInfo toInfo(PromptImage image) {
        String imageUrl = image.isReady()
                ? presignPromptImageDownloadPort.presignGet(image.getWatermarkedObjectKey())
                : null;
        return PromptImageStatusInfo.from(image, imageUrl);
    }

    /** HTTP 검증을 우회한 호출에서도 조회 개수·중복·요청자 정책을 보장 */
    private void validateQuery(GetPromptImageStatusesQuery query) {
        if (query == null
                || query.imageIds() == null
                || query.imageIds().isEmpty()
                || query.imageIds().size() > MAX_IMAGE_COUNT
                || query.imageIds().stream().anyMatch(java.util.Objects::isNull)) {
            throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_STATUS_QUERY_COUNT);
        }
        if (query.requesterId() == null || query.requesterId() <= 0) {
            throw new PromptDomainException(PromptErrorCode.INVALID_IMAGE_UPLOADER_ID);
        }
        if (new HashSet<>(query.imageIds()).size() != query.imageIds().size()) {
            throw new PromptDomainException(PromptErrorCode.DUPLICATE_IMAGE_ID);
        }
    }
}
