package com.paymentgateway.payments.infrastructure.persistence.repository;

import com.paymentgateway.payments.infrastructure.persistence.entity.OutboxEventEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {

    @Query(
            value =
                    """
                    WITH leased AS (
                        SELECT event_id
                        FROM outbox_events
                        WHERE status = 'PENDING'
                          AND next_attempt_at <= :now
                        ORDER BY next_attempt_at
                        LIMIT :limit
                        FOR UPDATE SKIP LOCKED
                    )
                    UPDATE outbox_events o
                    SET status = 'IN_PROGRESS',
                        attempt_count = o.attempt_count + 1,
                        updated_at = :now
                    FROM leased
                    WHERE o.event_id = leased.event_id
                    RETURNING o.event_id
                    """,
            nativeQuery = true)
    List<UUID> leaseReadyEvents(@Param("now") Instant now, @Param("limit") int limit);

    @Modifying
    @Query(
            value =
                    """
                    UPDATE outbox_events
                    SET status = 'PROCESSED',
                        updated_at = :now
                    WHERE event_id = :eventId
                      AND status = 'IN_PROGRESS'
                    """,
            nativeQuery = true)
    int markProcessed(@Param("eventId") UUID eventId, @Param("now") Instant now);

    @Modifying
    @Query(
            value =
                    """
                    UPDATE outbox_events
                    SET status = CASE
                                   WHEN attempt_count >= max_attempts THEN 'FAILED'
                                   ELSE 'PENDING'
                                 END,
                        next_attempt_at = CASE
                                            WHEN attempt_count >= max_attempts THEN next_attempt_at
                                            ELSE :nextAttemptAt
                                          END,
                        last_error_code = :errorCode,
                        last_error_message = :errorMessage,
                        updated_at = :now
                    WHERE event_id = :eventId
                      AND status = 'IN_PROGRESS'
                    """,
            nativeQuery = true)
    int markRetryableFailure(
            @Param("eventId") UUID eventId,
            @Param("errorCode") String errorCode,
            @Param("errorMessage") String errorMessage,
            @Param("nextAttemptAt") Instant nextAttemptAt,
            @Param("now") Instant now);

    @Modifying
    @Query(
            value =
                    """
                    UPDATE outbox_events
                    SET status = 'FAILED',
                        last_error_code = :errorCode,
                        last_error_message = :errorMessage,
                        updated_at = :now
                    WHERE event_id = :eventId
                      AND status = 'IN_PROGRESS'
                    """,
            nativeQuery = true)
    int markTerminalFailure(
            @Param("eventId") UUID eventId,
            @Param("errorCode") String errorCode,
            @Param("errorMessage") String errorMessage,
            @Param("now") Instant now);
}
