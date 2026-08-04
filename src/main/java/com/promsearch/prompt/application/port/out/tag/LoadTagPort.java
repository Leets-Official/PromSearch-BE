package com.promsearch.prompt.application.port.out.tag;

import com.promsearch.prompt.domain.Tag;
import com.promsearch.prompt.domain.enums.TagType;
import java.util.Collection;
import java.util.List;

public interface LoadTagPort {

    /*
     * 프롬프트 생성/임시저장처럼 요청자가 보낸 태그 ID가 모두 실제로 존재해야 하는 흐름에서 사용합니다.
     * 하나라도 누락되면 도메인 예외로 막아 잘못된 태그 연결이 저장되지 않게 합니다.
     */
    List<Tag> batchGetByIds(Collection<Long> tagIds);

    /*
     * 홈 필터 선택지처럼 "현재 등록된 태그 목록"만 보여주는 읽기 전용 조회입니다.
     * #65 정책에 따라 이 메서드는 태그를 새로 만들지 않고, 타입별 기존 태그만 반환합니다.
     */
    List<Tag> listByType(TagType tagType);
}
