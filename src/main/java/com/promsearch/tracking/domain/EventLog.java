package com.promsearch.tracking.domain;

import com.promsearch.tracking.domain.enums.EventName;
import com.promsearch.tracking.domain.enums.EventTargetType;
import com.promsearch.tracking.domain.exception.TrackingDomainException;
import com.promsearch.tracking.domain.exception.TrackingErrorCode;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class EventLog {

    private final EventLogId eventLogId;
    private final Long userId;
    private final EventName eventName;
    private final String anonymousId;
    private final String sessionId;
    private final EventTargetType targetType;
    private final Long targetId;
    private final String properties;
    private final Instant createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private EventLog(
            EventLogId eventLogId,
            Long userId,
            EventName eventName,
            String anonymousId,
            String sessionId,
            EventTargetType targetType,
            Long targetId,
            String properties,
            Instant createdAt
    ) {
        this.eventLogId = eventLogId;
        this.userId = userId;
        this.eventName = eventName;
        this.anonymousId = anonymousId;
        this.sessionId = sessionId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.properties = properties;
        this.createdAt = createdAt;
    }

    public static EventLog create(
            Long userId,
            EventName eventName,
            String anonymousId,
            String sessionId,
            EventTargetType targetType,
            Long targetId,
            String properties
    ) {
        validateRequired(userId, eventName, sessionId, targetType, targetId);

        return EventLog.builder()
                .userId(userId)
                .eventName(eventName)
                .anonymousId(anonymousId)
                .sessionId(sessionId)
                .targetType(targetType)
                .targetId(targetId)
                .properties(properties)
                .createdAt(Instant.now())
                .build();
    }

    public static EventLog reconstruct(
            EventLogId eventLogId,
            Long userId,
            EventName eventName,
            String anonymousId,
            String sessionId,
            EventTargetType targetType,
            Long targetId,
            String properties,
            Instant createdAt
    ) {
        validateRequired(userId, eventName, sessionId, targetType, targetId);

        return EventLog.builder()
                .eventLogId(eventLogId)
                .userId(userId)
                .eventName(eventName)
                .anonymousId(anonymousId)
                .sessionId(sessionId)
                .targetType(targetType)
                .targetId(targetId)
                .properties(properties)
                .createdAt(createdAt)
                .build();
    }

    private static void validateRequired(
            Long userId,
            EventName eventName,
            String sessionId,
            EventTargetType targetType,
            Long targetId
    ) {
        if (userId != null && userId <= 0) {
            throw new TrackingDomainException(TrackingErrorCode.INVALID_USER_ID);
        }
        if (eventName == null) {
            throw new TrackingDomainException(TrackingErrorCode.INVALID_EVENT_NAME);
        }
        if (sessionId == null || sessionId.isBlank()) {
            throw new TrackingDomainException(TrackingErrorCode.INVALID_SESSION_ID);
        }
        if (targetId != null && targetId <= 0) {
            throw new TrackingDomainException(TrackingErrorCode.INVALID_TARGET_ID);
        }
    }

    public record EventLogId(Long id) {
        public EventLogId {
            if (id == null || id <= 0) {
                throw new TrackingDomainException(TrackingErrorCode.INVALID_ID);
            }
        }
    }
}
