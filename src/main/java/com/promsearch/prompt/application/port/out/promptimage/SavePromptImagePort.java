package com.promsearch.prompt.application.port.out.promptimage;

import com.promsearch.prompt.domain.PromptImage;
import java.util.List;

/** 이미지 자산 저장 포트 */
public interface SavePromptImagePort {

    /** 업로드 준비 이미지 일괄 생성 */
    void createAll(List<PromptImage> images);

    /** 상태 전이 이미지 갱신 */
    PromptImage update(PromptImage image);

    /** 프롬프트 연결 정보를 이미지 전체에 일괄 반영 */
    void updateAll(List<PromptImage> images);
}
