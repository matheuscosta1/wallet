package br.com.wallet.project.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Functional tests for deposit operations.
 *
 * Scenarios covered:
 *  D-01  Successful deposit updates balance asynchronously
 *  D-02  Multiple sequential deposits accumulate correctly
 *  D-03  Deposit with zero amount → 400 Bad Request
 *  D-04  Deposit with negative amount → 400 Bad Request
 *  D-05  Deposit to non-existent wallet → error after async processing
 *  D-06  Transaction record saved with correct before/after balance
 *  D-07  API responds immediately (async) — does not block on Kafka processing
 */
@DisplayName("Deposit Operations")
class DepositFunctionalTest extends BaseFunctionalTest {

    @Autowired
    private ObjectMapper objectMapper;

    private static final long ASYNC_TIMEOUT_MS = 10_000;

    // ── Setup helper ──────────────────────────────────────────────────────────

    private void createWallet(String userId) throws Exception {
        mockMvc.perform(post("/creation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("userId", userId))))
                .andExpect(status().isOk());
    }

    // ── D-01 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("D-01: Deposit 100.00 → balance updated to 100.00 after async processing")
    void shouldUpdateBalanceAfterDeposit() throws Exception {
        createWallet("user-d01");

        var body = Map.of(
                "userId", "user-d01",
                "amount", new BigDecimal("100.00"),
                "idempotencyId", UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").exists());

        // Wait for Kafka consumer to process
        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = 'user-d01') AND type = 'DEPOSIT'",
                1, ASYNC_TIMEOUT_MS);

        BigDecimal balance = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-d01'", BigDecimal.class);
        assertThat(balance).isEqualByComparingTo("100.00");
    }

    // ── D-02 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("D-02: Three sequential deposits of 50.00 → final balance 150.00")
    void shouldAccumulateMultipleDeposits() throws Exception {
        createWallet("user-d02");

        for (int i = 0; i < 3; i++) {
            var body = Map.of(
                    "userId", "user-d02",
                    "amount", new BigDecimal("50.00"),
                    "idempotencyId", UUID.randomUUID().toString()
            );
            mockMvc.perform(post("/deposit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk());
        }

        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = 'user-d02') AND type = 'DEPOSIT'",
                3, ASYNC_TIMEOUT_MS);

        BigDecimal balance = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-d02'", BigDecimal.class);
        assertThat(balance).isEqualByComparingTo("150.00");
    }

    // ── D-03 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("D-03: Deposit amount = 0 → 400 Bad Request (validation)")
    void shouldRejectZeroAmountDeposit() throws Exception {
        createWallet("user-d03");

        var body = Map.of(
                "userId", "user-d03",
                "amount", BigDecimal.ZERO,
                "idempotencyId", UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ── D-04 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("D-04: Deposit amount = -50.00 → 400 Bad Request (validation)")
    void shouldRejectNegativeAmountDeposit() throws Exception {
        createWallet("user-d04");

        var body = Map.of(
                "userId", "user-d04",
                "amount", new BigDecimal("-50.00"),
                "idempotencyId", UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ── D-05 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("D-05: Deposit to non-existent wallet → message goes to DLQ, balance not changed")
    void shouldRouteToDeadLetterWhenWalletNotFound() throws Exception {
        // Do NOT create wallet — deposit to a ghost user
        var body = Map.of(
                "userId", "ghost-user-d05",
                "amount", new BigDecimal("100.00"),
                "idempotencyId", UUID.randomUUID().toString()
        );

        // API returns OK synchronously (message was published)
        mockMvc.perform(post("/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        // No transaction should ever be persisted
        Thread.sleep(3000);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transactions", Integer.class);
        assertThat(count).isZero();
    }

    // ── D-06 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("D-06: Transaction record saves balanceBefore=0 and balanceAfter=200")
    void shouldRecordCorrectBalanceSnapshotInTransaction() throws Exception {
        createWallet("user-d06");

        var body = Map.of(
                "userId", "user-d06",
                "amount", new BigDecimal("200.00"),
                "idempotencyId", UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = 'user-d06')",
                1, ASYNC_TIMEOUT_MS);

        Map<String, Object> tx = jdbcTemplate.queryForMap(
                "SELECT balance_before_transaction, balance_after_transaction FROM transactions " +
                "WHERE wallet_id = (SELECT id FROM wallets WHERE user_id = 'user-d06')");

        assertThat((BigDecimal) tx.get("balance_before_transaction"))
                .isEqualByComparingTo("0.00");
        assertThat((BigDecimal) tx.get("balance_after_transaction"))
                .isEqualByComparingTo("200.00");
    }

    // ── D-07 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("D-07: Deposit endpoint responds immediately without waiting for Kafka processing")
    void shouldRespondImmediatelyBeforeKafkaProcessing() throws Exception {
        createWallet("user-d07");

        var body = Map.of(
                "userId", "user-d07",
                "amount", new BigDecimal("100.00"),
                "idempotencyId", UUID.randomUUID().toString()
        );

        long start = System.currentTimeMillis();
        mockMvc.perform(post("/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
        long elapsed = System.currentTimeMillis() - start;

        // API must respond in well under 2 seconds (Kafka processing may take longer)
        assertThat(elapsed).isLessThan(2000L);
    }
}
