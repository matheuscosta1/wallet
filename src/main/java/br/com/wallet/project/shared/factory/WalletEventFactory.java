package br.com.wallet.project.shared.factory;

import br.com.wallet.project.application.command.TransactionCommand;
import br.com.wallet.project.domain.event.WalletEvent;
import br.com.wallet.project.domain.event.WalletEventType;
import br.com.wallet.project.domain.model.TransactionMessage;
import br.com.wallet.project.domain.model.Wallet;
import br.com.wallet.project.domain.model.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Centralised factory for building {@link WalletEvent} instances.
 *
 * <p><strong>Aggregate boundary rules:</strong>
 * <pre>
 *   aggregateType = "WALLET"
 *     aggregateId  = userId
 *     events       = WALLET_CREATED, DEPOSIT_*, WITHDRAW_*
 *
 *   aggregateType = "TRANSFER"
 *     aggregateId  = transactionId   ← the transfer owns its own identity
 *     events       = TRANSFER_*
 *
 *   aggregateType = "WALLET" | "TRANSFER"  (derived from TransactionMessage.type)
 *     events       = *_PERMANENTLY_FAILED  (in @DltHandler)
 * </pre>
 *
 * <p>Using {@code transactionId} as the aggregate for transfers means:
 * <ul>
 *   <li>One query gives the full lifecycle of a transfer (requested → completed/failed).</li>
 *   <li>Wallet events (deposits/withdraws) for the two participants are kept in their own
 *       wallet aggregates and are linked via {@code correlationId = idempotencyId}.</li>
 *   <li>No aggregate spans two wallets — each {@code WITHDRAW_COMPLETED} and
 *       {@code DEPOSIT_COMPLETED} belong to their respective wallet aggregates.</li>
 * </ul>
 */
public class WalletEventFactory {

    private WalletEventFactory() {}

    private static final String WALLET_TYPE   = "WALLET";
    private static final String TRANSFER_TYPE = "TRANSFER";

    // ── Wallet lifecycle ──────────────────────────────────────────────────────

    public static WalletEvent walletCreated(String userId, UUID correlationId) {
        return base(WalletEventType.WALLET_CREATED, WALLET_TYPE, userId, correlationId, null)
                .payload(Map.of("userId", userId))
                .build();
    }

    // ── Deposit ───────────────────────────────────────────────────────────────

    public static WalletEvent depositRequested(TransactionCommand cmd) {
        return base(WalletEventType.DEPOSIT_REQUESTED, WALLET_TYPE, cmd.getUserId(),
                cmd.getIdempotencyId(), null)
                .payload(transactionPayload(cmd))
                .build();
    }

    public static WalletEvent depositCompleted(TransactionCommand cmd, Wallet wallet,
                                               BigDecimal balanceBefore, UUID causationId) {
        return base(WalletEventType.DEPOSIT_COMPLETED, WALLET_TYPE, cmd.getUserId(),
                cmd.getIdempotencyId(), causationId)
                .payload(transactionPayloadWithBalance(cmd, wallet, balanceBefore, wallet.getBalance()))
                .build();
    }

    // ── Withdraw ──────────────────────────────────────────────────────────────

    public static WalletEvent withdrawRequested(TransactionCommand cmd) {
        return base(WalletEventType.WITHDRAW_REQUESTED, WALLET_TYPE, cmd.getUserId(),
                cmd.getIdempotencyId(), null)
                .payload(transactionPayload(cmd))
                .build();
    }

    public static WalletEvent withdrawCompleted(TransactionCommand cmd, Wallet wallet,
                                                BigDecimal balanceBefore, UUID causationId) {
        return base(WalletEventType.WITHDRAW_COMPLETED, WALLET_TYPE, cmd.getUserId(),
                cmd.getIdempotencyId(), causationId)
                .payload(transactionPayloadWithBalance(cmd, wallet, balanceBefore, wallet.getBalance()))
                .build();
    }

    // ── Transfer ──────────────────────────────────────────────────────────────
    // aggregateType = "TRANSFER", aggregateId = transactionId
    // The two participant wallets each get their own WITHDRAW_COMPLETED / DEPOSIT_COMPLETED
    // inside their respective WALLET aggregates (emitted by WithdrawStrategy / DepositStrategy).

    public static WalletEvent transferRequested(TransactionCommand cmd) {
        return base(WalletEventType.TRANSFER_REQUESTED, TRANSFER_TYPE,
                cmd.getTransactionId().toString(),
                cmd.getIdempotencyId(), null)
                .payload(Map.of(
                        "transactionId", String.valueOf(cmd.getTransactionId()),
                        "fromUserId",    cmd.getFromUserWalletId(),
                        "toUserId",      cmd.getToUserWalletId(),
                        "amount",        money(cmd.getAmount())
                ))
                .build();
    }

    public static WalletEvent transferCompleted(TransactionCommand cmd,
                                                BigDecimal fromBalanceBefore,
                                                BigDecimal fromBalanceAfter,
                                                BigDecimal toBalanceBefore,
                                                BigDecimal toBalanceAfter,
                                                UUID causationId) {
        return base(WalletEventType.TRANSFER_COMPLETED, TRANSFER_TYPE,
                cmd.getTransactionId().toString(),
                cmd.getIdempotencyId(), causationId)
                .payload(Map.of(
                        "transactionId",     String.valueOf(cmd.getTransactionId()),
                        "fromUserId",        cmd.getFromUserWalletId(),
                        "toUserId",          cmd.getToUserWalletId(),
                        "amount",            money(cmd.getAmount()),
                        "fromBalanceBefore", money(fromBalanceBefore),
                        "fromBalanceAfter",  money(fromBalanceAfter),
                        "toBalanceBefore",   money(toBalanceBefore),
                        "toBalanceAfter",    money(toBalanceAfter)
                ))
                .build();
    }

    public static WalletEvent transferFailed(TransactionCommand cmd, String reason,
                                             UUID causationId) {
        return base(WalletEventType.TRANSFER_FAILED, TRANSFER_TYPE,
                cmd.getTransactionId().toString(),
                cmd.getIdempotencyId(), causationId)
                .payload(Map.of(
                        "transactionId", String.valueOf(cmd.getTransactionId()),
                        "fromUserId",    cmd.getFromUserWalletId(),
                        "toUserId",      cmd.getToUserWalletId(),
                        "amount",        money(cmd.getAmount()),
                        "reason",        reason
                ))
                .build();
    }

    // ── Idempotency ───────────────────────────────────────────────────────────

    public static WalletEvent idempotencyDuplicateDetected(TransactionCommand cmd) {
        boolean isTransfer = cmd.getTransactionType() == TransactionType.TRANSFER;
        String aggType = isTransfer ? TRANSFER_TYPE : WALLET_TYPE;
        String aggId   = isTransfer
                ? String.valueOf(cmd.getTransactionId())
                : cmd.getUserId();

        return base(WalletEventType.IDEMPOTENCY_DUPLICATE_DETECTED, aggType, aggId,
                cmd.getIdempotencyId(), null)
                .payload(Map.of(
                        "transactionId",   String.valueOf(cmd.getTransactionId()),
                        "idempotencyId",   String.valueOf(cmd.getIdempotencyId()),
                        "transactionType", String.valueOf(cmd.getTransactionType())
                ))
                .build();
    }


    // ── DLQ — emitted only from @DltHandler, once per message ────────────────

    /**
     * Emitted once, inside {@code @DltHandler}, after ALL Kafka retries for a
     * deposit are exhausted. The {@code event_type} field itself now carries the
     * specific failure type — no need for a {@code failedEventType} payload field.
     */
    public static WalletEvent depositPermanentlyFailed(TransactionMessage message,
                                                        String reason,
                                                        int retryCount) {
        return permanentlyFailed(WalletEventType.DEPOSIT_PERMANENTLY_FAILED,
                WALLET_TYPE,
                message.getUserId() != null ? message.getUserId() : message.getFromUserId(),
                message, reason, retryCount);
    }

    /**
     * Emitted once, inside {@code @DltHandler}, after ALL Kafka retries for a
     * withdrawal are exhausted.
     */
    public static WalletEvent withdrawPermanentlyFailed(TransactionMessage message,
                                                         String reason,
                                                         int retryCount) {
        return permanentlyFailed(WalletEventType.WITHDRAW_PERMANENTLY_FAILED,
                WALLET_TYPE,
                message.getUserId() != null ? message.getUserId() : message.getFromUserId(),
                message, reason, retryCount);
    }

    /**
     * Emitted once, inside {@code @DltHandler}, after ALL Kafka retries for a
     * transfer are exhausted. Uses {@code TRANSFER} aggregate with {@code transactionId}.
     */
    public static WalletEvent transferPermanentlyFailed(TransactionMessage message,
                                                         String reason,
                                                         int retryCount) {
        return permanentlyFailed(WalletEventType.TRANSFER_PERMANENTLY_FAILED,
                TRANSFER_TYPE,
                String.valueOf(message.getTransactionId()),
                message, reason, retryCount);
    }

    private static WalletEvent permanentlyFailed(WalletEventType type,
                                                   String aggType,
                                                   String aggId,
                                                   TransactionMessage message,
                                                   String reason,
                                                   int retryCount) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("transactionId",   String.valueOf(message.getTransactionId()));
        payload.put("idempotencyId",   String.valueOf(message.getIdempotencyId()));
        payload.put("transactionType", String.valueOf(message.getType()));
        payload.put("amount",          message.getAmount() != null
                ? money(message.getAmount()) : null);
        payload.put("userId",          message.getUserId());
        payload.put("fromUserId",      message.getFromUserId());
        payload.put("toUserId",        message.getToUserId());
        payload.put("reason",          reason);
        payload.put("retryCount",      retryCount);

        return base(type, aggType, aggId, message.getIdempotencyId(), null)
                .payload(payload)
                .build();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static WalletEvent.WalletEventBuilder base(WalletEventType type,
                                                       String aggregateType,
                                                       String aggregateId,
                                                       UUID correlationId,
                                                       UUID causationId) {
        return WalletEvent.builder()
                .eventId(UUID.randomUUID())
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(type)
                .eventVersion(1)
                .correlationId(correlationId)
                .causationId(causationId)
                .occurredAt(LocalDateTime.now());
    }

    /**
     * Formats a {@link BigDecimal} to exactly 2 decimal places (e.g. {@code 100} → {@code "100.00"})
     * before converting to a plain string, so all monetary values in event payloads
     * are consistently formatted regardless of the input scale.
     */
    private static String money(BigDecimal value) {
        return value.setScale(2, java.math.RoundingMode.HALF_DOWN).toPlainString();
    }

    private static Map<String, Object> transactionPayload(TransactionCommand cmd) {
        Map<String, Object> p = new HashMap<>();
        p.put("transactionId",   String.valueOf(cmd.getTransactionId()));
        p.put("userId",          cmd.getUserId());
        p.put("amount",          money(cmd.getAmount()));
        p.put("transactionType", String.valueOf(cmd.getTransactionType()));
        return p;
    }

    private static Map<String, Object> transactionPayloadWithBalance(TransactionCommand cmd,
                                                                     Wallet wallet,
                                                                     BigDecimal balanceBefore,
                                                                     BigDecimal balanceAfter) {
        Map<String, Object> p = transactionPayload(cmd);
        p.put("balanceBefore", money(balanceBefore));
        p.put("balanceAfter",  money(balanceAfter));
        p.put("walletId",      String.valueOf(wallet.getId()));
        return p;
    }
}