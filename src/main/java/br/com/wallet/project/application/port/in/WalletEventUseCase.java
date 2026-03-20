package br.com.wallet.project.application.port.in;

import br.com.wallet.project.domain.event.WalletEvent;
import br.com.wallet.project.domain.event.WalletEventType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Driving port: exposes event sourcing queries to the outside world.
 *
 * <p>Use these to replay an aggregate's history, trace a full request
 * across sync + async phases, or build audit reports.
 */
public interface WalletEventUseCase {

    /** Full history for a wallet (aggregate), oldest → newest. */
    List<WalletEvent> getAggregateHistory(String aggregateId);

    /** All events linked by a single HTTP/Kafka correlation chain. */
    List<WalletEvent> getEventsByCorrelationId(UUID correlationId);

    /** Retrieve a single event by its unique eventId. */
    Optional<WalletEvent> getEventById(UUID eventId);

    /** All events of a given type for an aggregate. */
    List<WalletEvent> getEventsByAggregateIdAndType(String aggregateId,
                                                     WalletEventType eventType);

    /** Range query — useful for external auditing tools. */
    List<WalletEvent> getEventsByDateRange(LocalDateTime start, LocalDateTime end);
}
