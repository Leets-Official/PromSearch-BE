package com.promsearch.user.application.usecase;

import com.promsearch.user.application.usecase.dto.IssueProfileImageUploadUrlCommand;
import com.promsearch.user.application.usecase.dto.ProfileImageUploadUrlInfo;

/**
 * 사용자 전용 프로필 이미지 Object Key와 Presigned PUT URL을 발급하는 유스케이스입니다.
 */
public interface IssueProfileImageUploadUrlUseCase {

    /**
     * 이미지 형식과 크기를 검증하고 S3 직접 업로드 정보를 발급합니다.
     *
     * @param command 사용자 ID와 업로드할 파일 정보
     * @return Object Key, Presigned PUT URL, 필수 서명 조건과 만료 시각
     */
    ProfileImageUploadUrlInfo issue(IssueProfileImageUploadUrlCommand command);
}
