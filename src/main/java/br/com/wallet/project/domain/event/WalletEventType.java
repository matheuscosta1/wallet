package br.com.wallet.project.domain.event;

/**
 * All possible domain events in the wallet system.
 * Each event represents something that definitively happened (past tense).
 *
 * <p><strong>Error event lifecycle for a failing message:</strong>
 * <pre>
 *   DEPOSIT_REQUESTED          (sync, 1x — published before Kafka)
 *   DEPOSIT_PERMANENTLY_FAILED (async, 1x — after all retries exhausted, in @DltHandler)
 * </pre>
 */
public enum WalletEventType {

    WALLET_CREATED,
    DEPOSIT_REQUESTED,
    DEPOSIT_COMPLETED,
    /**
     * Emitted exactly once, in {@code @DltHandler}, after ALL Kafka retries for a
     * deposit are exhausted. The definitive "this deposit will never be processed" record.
     */
    DEPOSIT_PERMANENTLY_FAILED,

    // ── Withdraw ──────────────────────────────────────────────────────────────
    WITHDRAW_REQUESTED,
    WITHDRAW_COMPLETED,
    /**
     * Emitted exactly once, in {@code @DltHandler}, after ALL Kafka retries for a
     * withdrawal are exhausted.
     */
    WITHDRAW_PERMANENTLY_FAILED,

    // ── Transfer ──────────────────────────────────────────────────────────────
    TRANSFER_REQUESTED,
    TRANSFER_COMPLETED,
    TRANSFER_FAILED,
    /**
     * Emitted exactly once, in {@code @DltHandler}, after ALL Kafka retries for a
     * transfer are exhausted.
     */
    TRANSFER_PERMANENTLY_FAILED,

    /**
     * Emitted on every consumer attempt where Redis detects a duplicate idempotencyId.
     * Since the exception triggers Kafka retries, multiple of these may appear.
     */
    IDEMPOTENCY_DUPLICATE_DETECTED,

}

