package com.promsearch.prompt.application.port.out.storage;

/** 이미지 객체 삭제 포트 */
public interface DeletePromptImageObjectPort {

    /** Object Key 기반 멱등 삭제 */
    void delete(String objectKey);
}
