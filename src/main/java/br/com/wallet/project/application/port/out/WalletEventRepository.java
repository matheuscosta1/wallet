package br.com.wallet.project.application.port.out;

import br.com.wallet.project.domain.event.WalletEvent;
import br.com.wallet.project.domain.event.WalletEventType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Driven port: append-only event store contract.
 *
 * <p>Callers only ever append events — never update or delete them.
 * Querying is used for projections, audits, and replay.
 */
public interface WalletEventRepository {

    /** Append a single event. Idempotent: duplicate eventIds are silently ignored. */
    WalletEvent save(WalletEvent event);

    /** Replay the full history of an aggregate in chronological order. */
    List<WalletEvent> findByAggregateId(String aggregateId);

    /** Retrieve all events linked by the same HTTP/Kafka correlation. */
    List<WalletEvent> findByCorrelationId(UUID correlationId);

    /** Find a single event by its unique eventId. */
    Optional<WalletEvent> findByEventId(UUID eventId);

    /** All events for an aggregate of a specific type. */
    List<WalletEvent> findByAggregateIdAndEventType(String aggregateId, WalletEventType eventType);

    /** Range query — useful for rebuilding projections or auditing a window. */
    List<WalletEvent> findByDateRange(LocalDateTime start, LocalDateTime end);

    /** Guard against double-persisting the same event. */
    boolean existsByEventId(UUID eventId);
}
