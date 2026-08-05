package com.promsearch.user.application.usecase;

import com.promsearch.user.application.usecase.dto.CompleteProfileImageUploadCommand;
import com.promsearch.user.application.usecase.dto.UserInfo;

/**
 * 저장소 직접 업로드가 끝난 프로필 이미지를 사용자 계정에 연결하는 입력 포트.
 */
public interface CompleteProfileImageUploadUseCase {

    /**
     * 객체 소유권과 실제 저장 메타데이터를 검증한 뒤 사용자의 프로필 이미지를 교체한다.
     *
     * @param command 사용자, 객체 키, MIME 타입, 파일 크기를 담은 명령
     * @return 교체된 프로필 이미지 URL을 포함한 사용자 정보
     */
    UserInfo complete(CompleteProfileImageUploadCommand command);
}
