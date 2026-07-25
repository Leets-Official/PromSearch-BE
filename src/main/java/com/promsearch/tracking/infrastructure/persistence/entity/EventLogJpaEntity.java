package com.promsearch.tracking.infrastructure.persistence.entity;

import com.promsearch.common.BaseEntity;
import com.promsearch.tracking.domain.EventLog;
import com.promsearch.tracking.domain.EventLog.EventLogId;
import com.promsearch.tracking.domain.enums.EventName;
import com.promsearch.tracking.domain.enums.EventTargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "event_logs")
public class EventLogJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_name", nullable = false, length = 100)
    private EventName eventName;

    @Column(name = "anonymous_id", length = 100)
    private String anonymousId;

    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 30)
    private EventTargetType targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "properties", columnDefinition = "TEXT")
    private String properties;

    @Builder(access = AccessLevel.PRIVATE)
    private EventLogJpaEntity(Long userId, EventName eventName, String anonymousId, String sessionId,
                              EventTargetType targetType, Long targetId, String properties) {
        this.userId = userId;
        this.eventName = eventName;
        this.anonymousId = anonymousId;
        this.sessionId = sessionId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.properties = properties;
    }

    public static EventLogJpaEntity create(Long userId, EventName eventName, String anonymousId, String sessionId,
                                           EventTargetType targetType, Long targetId, String properties) {
        return EventLogJpaEntity.builder()
                .userId(userId)
                .eventName(eventName)
                .anonymousId(anonymousId)
                .sessionId(sessionId)
                .targetType(targetType)
                .targetId(targetId)
                .properties(properties)
                .build();
    }

    public EventLog toDomain() {
        return EventLog.reconstruct(
                new EventLogId(id),
                userId,
                eventName,
                anonymousId,
                sessionId,
                targetType,
                targetId,
                properties,
                getCreatedAt()
        );
    }
}
