package br.com.wallet.project.adapter.out.persistence.jpa;

import br.com.wallet.project.adapter.out.persistence.entity.WalletEventEntity;
import br.com.wallet.project.domain.event.WalletEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaWalletEventRepository extends JpaRepository<WalletEventEntity, Long> {

    /** Replay all events for a single aggregate, ordered chronologically. */
    List<WalletEventEntity> findByAggregateIdOrderByOccurredAtAsc(String aggregateId);

    /** Find a specific event by its globally unique eventId. */
    Optional<WalletEventEntity> findByEventId(UUID eventId);

    /** All events in a date range — useful for audits and projections rebuilds. */
    @Query("""
            SELECT e FROM WalletEventEntity e
            WHERE e.occurredAt >= :start
              AND e.occurredAt <= :end
            ORDER BY e.occurredAt ASC
            """)
    List<WalletEventEntity> findByDateRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /** Trace all events that share the same correlationId (full request trace). */
    List<WalletEventEntity> findByCorrelationIdOrderByOccurredAtAsc(UUID correlationId);

    /** All events for an aggregate filtered by type (e.g. all DEPOSITs for a user). */
    List<WalletEventEntity> findByAggregateIdAndEventTypeOrderByOccurredAtAsc(
            String aggregateId, WalletEventType eventType);

    /** Check dedup by eventId before persisting. */
    boolean existsByEventId(UUID eventId);
}
