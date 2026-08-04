package com.promsearch.prompt.application.service.query;

import com.promsearch.prompt.application.port.out.tag.LoadTagPort;
import com.promsearch.prompt.application.usecase.ListPromptTagsUseCase;
import com.promsearch.prompt.application.usecase.dto.PromptTagInfo;
import com.promsearch.prompt.domain.enums.TagType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromptTagQueryService implements ListPromptTagsUseCase {

    private final LoadTagPort loadTagPort;

    @Override
    public List<PromptTagInfo> listByType(TagType tagType) {
        /*
         * 홈 필터 선택지는 #65 회원가입 프로필 태그와 같은 tags 테이블을 바라봅니다.
         * 사용자가 필터를 열 때마다 태그를 만들면 중복/표기 흔들림이 생기므로,
         * 조회 서비스에서는 LoadTagPort만 사용해 이미 저장된 태그를 읽기만 합니다.
         */
        return loadTagPort.listByType(tagType).stream()
                .map(PromptTagInfo::from)
                .toList();
    }
}
