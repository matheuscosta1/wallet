package br.com.wallet.project.domain.event;

import lombok.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable domain event for the wallet aggregate.
 * Every state change in the system produces one or more WalletEvents.
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WalletEvent {

    /** Unique identifier for this specific event instance. */
    private UUID eventId;

    /**
     * The aggregate this event belongs to.
     * For wallet operations: userId.
     * For transfers: fromUserId (the initiating wallet).
     */
    private String aggregateId;

    /** Always "WALLET" for this system. */
    @Builder.Default
    private String aggregateType = "WALLET";

    /** What happened. */
    private WalletEventType eventType;

    /** Schema version — increment when payload structure changes. */
    @Builder.Default
    private int eventVersion = 1;

    /**
     * The event payload — all domain-relevant data.
     * Stored as a flat Map so it serializes cleanly to JSONB without
     * depending on a specific class name in the DB.
     */
    private Map<String, Object> payload;

    /**
     * Technical metadata: partition, offset, threadName, etc.
     * Optional — useful for debugging and auditing.
     */
    private Map<String, Object> metadata;

    /**
     * Ties the HTTP request → Kafka message → consumer processing together.
     * Set once at the controller layer and propagated through the entire chain.
     */
    private UUID correlationId;

    /**
     * The eventId that caused this event.
     * E.g. DEPOSIT_REQUESTED causes DEPOSIT_COMPLETED.
     */
    private UUID causationId;

    /** When the event actually occurred in the domain. */
    private LocalDateTime occurredAt;
}
