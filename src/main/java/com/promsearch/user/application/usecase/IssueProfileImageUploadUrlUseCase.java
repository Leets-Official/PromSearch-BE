package com.promsearch.user.application.usecase;

import com.promsearch.user.application.usecase.dto.IssueProfileImageUploadUrlCommand;
import com.promsearch.user.application.usecase.dto.ProfileImageUploadUrlInfo;

/**
 * 프로필 이미지 직접 업로드를 시작하는 입력 포트.
 */
public interface IssueProfileImageUploadUrlUseCase {

    /**
     * 요청 파일을 검증하고 사용자 전용 객체 키와 서명된 업로드 URL을 발급한다.
     *
     * @param command 사용자, MIME 타입, 파일 크기를 담은 명령
     * @return 객체 키, 업로드 URL, 만료 시각
     */
    ProfileImageUploadUrlInfo issue(IssueProfileImageUploadUrlCommand command);
}
