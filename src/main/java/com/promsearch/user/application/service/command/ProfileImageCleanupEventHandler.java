package com.promsearch.user.application.service.command;

import com.promsearch.user.application.port.out.profileimage.DeleteProfileImageObjectPort;
import com.promsearch.user.application.usecase.dto.ProfileImageCleanupEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 프로필 이미지 DB 변경이 커밋된 후 이전 저장소 객체를 삭제합니다.
 * 삭제 실패는 이미 완료된 DB 트랜잭션에 영향을 주지 않도록 로그로 남깁니다.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ProfileImageCleanupEventHandler {

    private final DeleteProfileImageObjectPort deleteProfileImageObjectPort;

    /**
     * @param event 더 이상 참조되지 않는 프로필 이미지 Object Key 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void deleteAfterCommit(ProfileImageCleanupEvent event) {
        try {
            deleteProfileImageObjectPort.delete(event.objectKey());
        } catch (RuntimeException e) {
            // DB 커밋은 완료됐으므로 사용자 요청을 실패로 되돌리지 않고 운영 로그로 후속 정리 대상을 남긴다.
            log.error("profile_image_cleanup_failed objectKey={} errorType={}",
                    event.objectKey(), e.getClass().getSimpleName());
        }
    }
}
