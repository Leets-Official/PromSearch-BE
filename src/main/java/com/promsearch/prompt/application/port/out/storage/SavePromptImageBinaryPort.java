package com.promsearch.prompt.application.port.out.storage;

/** 워터마크 처리 결과 저장 포트 */
public interface SavePromptImageBinaryPort {

    /** 결과 Object Key에 지정한 Content-Type으로 이미지 저장 */
    void save(String objectKey, String contentType, byte[] bytes);
}
