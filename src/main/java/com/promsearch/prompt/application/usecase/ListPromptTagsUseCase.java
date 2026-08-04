package com.promsearch.prompt.application.usecase;

import com.promsearch.prompt.application.usecase.dto.PromptTagInfo;
import com.promsearch.prompt.domain.enums.TagType;
import java.util.List;

/*
 * 홈 화면 필터에서 사용할 태그 선택지를 조회하는 입력 포트입니다.
 * 태그 생성은 업로드/회원가입 쪽 정책에 따르고, 이 유스케이스는 저장된 태그를 읽기만 합니다.
 */
public interface ListPromptTagsUseCase {

    List<PromptTagInfo> listByType(TagType tagType);
}
