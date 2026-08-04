package com.promsearch.user.application.port.out.profileimage;

/**
 * DB 변경이 확정된 뒤 기존 프로필 이미지 삭제를 실행하도록 예약하는 출력 포트.
 *
 * <p>트랜잭션이 롤백됐는데 기존 S3 객체만 먼저 삭제되는 불일치를 방지한다.</p>
 */
public interface ScheduleProfileImageDeletionPort {

    /**
     * 현재 트랜잭션이 커밋된 후 객체를 삭제한다.
     *
     * <p>활성 트랜잭션 동기화가 없다면 즉시 삭제하며, 값이 없으면 아무 작업도 하지 않는다.</p>
     *
     * @param objectKey 삭제할 기존 프로필 이미지 객체 키
     */
    void afterCommit(String objectKey);
}
