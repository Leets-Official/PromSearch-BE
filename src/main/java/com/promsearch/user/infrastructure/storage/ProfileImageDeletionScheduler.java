package com.promsearch.user.infrastructure.storage;

import com.promsearch.user.application.port.out.profileimage.ProfileImageStoragePort;
import com.promsearch.user.application.port.out.profileimage.ScheduleProfileImageDeletionPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 사용자 DB 트랜잭션이 커밋된 뒤 기존 프로필 이미지 객체를 정리하는 어댑터.
 *
 * <p>외부 저장소 삭제 실패는 이미 완료된 DB 트랜잭션을 되돌릴 수 없으므로 로그를 남기고
 * 요청 처리는 유지한다.</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ProfileImageDeletionScheduler implements ScheduleProfileImageDeletionPort {

    private final ProfileImageStoragePort profileImageStoragePort;

    /**
     * 트랜잭션 동기화가 활성화되어 있으면 커밋 콜백을 등록하고, 그렇지 않으면 즉시 삭제한다.
     */
    @Override
    public void afterCommit(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteQuietly(objectKey);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteQuietly(objectKey);
            }
        });
    }

    /**
     * 외부 저장소 정리 실패가 이미 커밋된 사용자 변경 결과에 영향을 주지 않도록 격리한다.
     */
    private void deleteQuietly(String objectKey) {
        try {
            profileImageStoragePort.delete(objectKey);
        } catch (RuntimeException exception) {
            log.warn("profile_image_cleanup_failed objectKey={} errorType={}",
                    objectKey, exception.getClass().getSimpleName());
        }
    }
}
