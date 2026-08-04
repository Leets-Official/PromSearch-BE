package com.promsearch.user.application.port.out.profileimage;

/**
 * 프로필 이미지 저장소에서 객체를 삭제하는 출력 포트입니다.
 */
public interface DeleteProfileImageObjectPort {

    /**
     * 지정한 Object Key의 객체를 삭제합니다. 이미 존재하지 않는 객체는 성공으로 처리합니다.
     *
     * @param objectKey 삭제할 프로필 이미지 Object Key
     */
    void delete(String objectKey);
}
