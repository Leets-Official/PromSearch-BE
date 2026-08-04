package com.promsearch.user.application.usecase;

import com.promsearch.user.application.usecase.dto.CompleteProfileImageUploadCommand;
import com.promsearch.user.application.usecase.dto.ProfileImageInfo;

/**
 * S3 직접 업로드를 완료하고 해당 객체를 사용자의 프로필 이미지로 적용하는 유스케이스입니다.
 */
public interface CompleteProfileImageUploadUseCase {

    /**
     * Object Key 소유권과 저장 객체 메타데이터를 검증한 후 프로필 이미지를 교체합니다.
     * 기존 자사 프로필 이미지 객체는 DB 트랜잭션 커밋 후 삭제됩니다.
     *
     * @param command 사용자 ID와 업로드 완료 Object Key
     * @return 적용된 프로필 이미지 공개 URL
     */
    ProfileImageInfo complete(CompleteProfileImageUploadCommand command);
}
