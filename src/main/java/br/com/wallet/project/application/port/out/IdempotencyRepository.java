package br.com.wallet.project.application.port.out;
/**
 * Driven port: idempotency check contract.
 * Prevents duplicate transaction processing.
 * Redis provides the implementation in the infrastructure layer.
 */
public interface IdempotencyRepository {
    /** Registers the key only if it does not exist. Returns true if first time (not duplicate). */
    boolean registerIfAbsent(String key);
    /** Marks the key as COMPLETED after successful processing. */
    void markAsCompleted(String key);
    /** Releases the key when processing fails. */
    void release(String key);
}
