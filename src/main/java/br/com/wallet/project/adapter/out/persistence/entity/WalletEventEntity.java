package br.com.wallet.project.adapter.out.persistence.entity;

import br.com.wallet.project.domain.event.WalletEventType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * JPA entity that persists a {@link br.com.wallet.project.domain.event.WalletEvent}
 * into the {@code wallet_events} append-only table.
 *
 * <p>This table is the authoritative event log. The {@code wallets} and
 * {@code transactions} tables are read-model projections kept in sync by the
 * domain strategies (DepositStrategy, WithdrawStrategy, TransferStrategy).
 */
@Entity
@Table(name = "wallet_events",
        indexes = {
                @Index(name = "idx_wallet_events_aggregate_id",   columnList = "aggregateId"),
                @Index(name = "idx_wallet_events_event_type",     columnList = "eventType"),
                @Index(name = "idx_wallet_events_occurred_at",    columnList = "occurredAt"),
                @Index(name = "idx_wallet_events_correlation_id", columnList = "correlationId"),
        })
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Globally unique event identifier — used for idempotency and dedup. */
    @Column(nullable = false, unique = true, updatable = false)
    private UUID eventId;

    /**
     * Logical aggregate identifier.
     * For DEPOSIT / WITHDRAW: the wallet owner's userId.
     * For TRANSFER: the fromUserId (initiating side).
     */
    @Column(nullable = false, updatable = false)
    private String aggregateId;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private String aggregateType = "WALLET";

    @Column(nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private WalletEventType eventType;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private int eventVersion = 1;

    /**
     * Domain payload stored as JSONB.
     * Contains the full business data for the event (amounts, userIds, balances…).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb", updatable = false)
    private Map<String, Object> payload;

    /**
     * Technical metadata stored as JSONB.
     * Contains Kafka partition/offset, thread name, etc.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", updatable = false)
    private Map<String, Object> metadata;

    /** Links HTTP request → Kafka publish → consumer processing in one trace. */
    @Column(updatable = false)
    private UUID correlationId;

    /** The eventId that triggered this event (request → completion chain). */
    @Column(updatable = false)
    private UUID causationId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
