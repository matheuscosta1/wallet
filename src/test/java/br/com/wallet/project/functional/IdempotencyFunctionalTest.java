package br.com.wallet.project.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Functional tests for idempotency enforcement via Redis.
 *
 * These tests validate that the system never processes the same
 * idempotencyId twice, even under concurrent or retry scenarios.
 *
 * Scenarios covered:
 *  I-01  Same idempotencyId sent twice sequentially → processed only once
 *  I-02  Same idempotencyId sent 5 times → exactly one transaction persisted
 *  I-03  Different idempotencyIds for same amount → processed independently
 *  I-04  Same idempotencyId for deposit sent concurrently by 10 threads → exactly one execution
 *  I-05  Same idempotencyId for transfer sent twice → only one transfer record persisted
 *  I-06  Same idempotencyId for withdraw sent twice → only one withdraw processed
 *  I-07  Idempotency key expires and same operation can be retried (TTL behavior)
 *  I-08  Null idempotencyId → 400 Bad Request (required field)
 *  I-09  Blank idempotencyId → 400 Bad Request
 */
@DisplayName("Idempotency — Redis-based Deduplication")
class IdempotencyFunctionalTest extends BaseFunctionalTest {

    @Autowired
    private ObjectMapper objectMapper;

    private static final long ASYNC_TIMEOUT_MS = 10_000;

    private void createWallet(String userId) throws Exception {
        mockMvc.perform(post("/creation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("userId", userId))))
                .andExpect(status().isOk());
    }

    private void deposit(String userId, String amount) throws Exception {
        var body = Map.of(
                "userId", userId,
                "amount", new BigDecimal(amount),
                "idempotencyId", UUID.randomUUID().toString()
        );
        mockMvc.perform(post("/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
        waitForExactBalance(userId, new BigDecimal(amount), ASYNC_TIMEOUT_MS);
    }

    // ── I-01 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("I-01: Same idempotencyId sent twice sequentially → only one transaction in DB")
    void shouldProcessDepositOnlyOnceForSameIdempotencyId() throws Exception {
        createWallet("user-i01");
        String idempotencyId = UUID.randomUUID().toString();

        var body = Map.of(
                "userId", "user-i01",
                "amount", new BigDecimal("100.00"),
                "idempotencyId", idempotencyId
        );

        // First request
        mockMvc.perform(post("/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = 'user-i01')",
                1, ASYNC_TIMEOUT_MS);

        // Second request — same idempotencyId
        mockMvc.perform(post("/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        // Wait and assert no second transaction was created
        Thread.sleep(3000);

        Integer txCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = 'user-i01')",
                Integer.class);
        assertThat(txCount).isEqualTo(1);

        BigDecimal balance = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-i01'", BigDecimal.class);
        assertThat(balance).isEqualByComparingTo("100.00"); // not 200.00
    }

    // ── I-02 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("I-02: Same idempotencyId sent 5 times → exactly 1 transaction persisted, balance = 50.00")
    void shouldIgnoreFourDuplicatesOutOfFiveRequests() throws Exception {
        createWallet("user-i02");
        String idempotencyId = UUID.randomUUID().toString();

        var body = Map.of(
                "userId", "user-i02",
                "amount", new BigDecimal("50.00"),
                "idempotencyId", idempotencyId
        );

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/deposit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk());
        }

        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = 'user-i02')",
                1, ASYNC_TIMEOUT_MS);

        // Extra wait to make sure no duplicate slips through
        Thread.sleep(2000);

        Integer txCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = 'user-i02')",
                Integer.class);
        assertThat(txCount).isEqualTo(1);

        BigDecimal balance = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-i02'", BigDecimal.class);
        assertThat(balance).isEqualByComparingTo("50.00");
    }

    // ── I-03 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("I-03: Two deposits with different idempotencyIds → both processed, balance = 200.00")
    void shouldProcessTwoDepositsWithDifferentIdempotencyIds() throws Exception {
        createWallet("user-i03");

        for (int i = 0; i < 2; i++) {
            var body = Map.of(
                    "userId", "user-i03",
                    "amount", new BigDecimal("100.00"),
                    "idempotencyId", UUID.randomUUID().toString() // different each time
            );
            mockMvc.perform(post("/deposit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk());
        }

        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = 'user-i03')",
                2, ASYNC_TIMEOUT_MS);

        BigDecimal balance = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-i03'", BigDecimal.class);
        assertThat(balance).isEqualByComparingTo("200.00");
    }

    // ── I-04 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("I-04: Same idempotencyId fired concurrently by 10 threads → exactly 1 transaction persisted")
    void shouldHandleConcurrentDuplicatesIdempotently() throws Exception {
        createWallet("user-i04");
        String idempotencyId = UUID.randomUUID().toString();

        var body = Map.of(
                "userId", "user-i04",
                "amount", new BigDecimal("100.00"),
                "idempotencyId", idempotencyId
        );
        String bodyJson = objectMapper.writeValueAsString(body);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // all threads start simultaneously
                    mockMvc.perform(post("/deposit")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(bodyJson))
                            .andExpect(status().isOk());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // ignore http-level errors
                }
            });
        }

        startLatch.countDown(); // release all threads at once
        executor.shutdown();
        executor.awaitTermination(15, java.util.concurrent.TimeUnit.SECONDS);

        // All requests may return 200 (async), but only ONE should be processed
        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = 'user-i04')",
                1, ASYNC_TIMEOUT_MS);

        Thread.sleep(2000); // extra guard

        Integer txCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = 'user-i04')",
                Integer.class);
        assertThat(txCount).isEqualTo(1);

        BigDecimal balance = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-i04'", BigDecimal.class);
        assertThat(balance).isEqualByComparingTo("100.00");
    }

    // ── I-05 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("I-05: Same idempotencyId for transfer sent twice → only 1 transfer record, balances correct")
    void shouldProcessTransferOnlyOnceForSameIdempotencyId() throws Exception {
        createWallet("user-i05-a");
        createWallet("user-i05-b");
        deposit("user-i05-a", "100.00");

        String idempotencyId = UUID.randomUUID().toString();
        var body = Map.of(
                "fromUserId", "user-i05-a",
                "toUserId", "user-i05-b",
                "amount", new BigDecimal("40.00"),
                "idempotencyId", idempotencyId
        );

        // Send twice
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk());
        }

        waitForCondition("SELECT COUNT(*) FROM transfers", 1, ASYNC_TIMEOUT_MS);
        Thread.sleep(2000);

        Integer transferCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transfers", Integer.class);
        assertThat(transferCount).isEqualTo(1);

        BigDecimal balanceA = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-i05-a'", BigDecimal.class);
        BigDecimal balanceB = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-i05-b'", BigDecimal.class);

        assertThat(balanceA).isEqualByComparingTo("60.00");
        assertThat(balanceB).isEqualByComparingTo("40.00");
    }

    // ── I-06 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("I-06: Same idempotencyId for withdraw sent twice → only 1 withdraw processed")
    void shouldProcessWithdrawOnlyOnceForSameIdempotencyId() throws Exception {
        createWallet("user-i06");
        deposit("user-i06", "100.00");

        String idempotencyId = UUID.randomUUID().toString();
        var body = Map.of(
                "userId", "user-i06",
                "amount", new BigDecimal("30.00"),
                "idempotencyId", idempotencyId
        );

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/withdraw")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk());
        }

        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = 'user-i06') AND type = 'WITHDRAW'",
                1, ASYNC_TIMEOUT_MS);

        Thread.sleep(2000);

        Integer txCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = 'user-i06') AND type = 'WITHDRAW'",
                Integer.class);
        assertThat(txCount).isEqualTo(1);

        BigDecimal balance = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-i06'", BigDecimal.class);
        assertThat(balance).isEqualByComparingTo("70.00"); // not 40.00
    }

    // ── I-07 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("I-07: Redis key manually expired → same idempotencyId processed as new request")
    void shouldReprocessAfterIdempotencyKeyExpires() throws Exception {
        createWallet("user-i07");
        String idempotencyId = "6a2ff239-6936-41e8-9f1a-9caecd1fd0ca";

        var body = Map.of(
                "userId", "user-i07",
                "amount", new BigDecimal("50.00"),
                "idempotencyId", idempotencyId
        );

        // First processing
        mockMvc.perform(post("/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = 'user-i07')",
                1, ASYNC_TIMEOUT_MS);

        // Manually expire all Redis keys (simulates TTL expiry)
        redisTemplate.delete(redisTemplate.keys("*"));

        // Second request after key expired — should be treated as new
        mockMvc.perform(post("/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = 'user-i07')",
                2, ASYNC_TIMEOUT_MS);

        BigDecimal balance = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-i07'", BigDecimal.class);
        assertThat(balance).isEqualByComparingTo("100.00");
    }

    // ── I-08 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("I-08: Null idempotencyId in deposit → 400 Bad Request")
    void shouldRejectNullIdempotencyId() throws Exception {
        createWallet("user-i08");

        var body = new java.util.HashMap<String, Object>();
        body.put("userId", "user-i08");
        body.put("amount", new BigDecimal("100.00"));
        body.put("idempotencyId", null);

        mockMvc.perform(post("/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ── I-09 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("I-09: Blank idempotencyId in deposit → 400 Bad Request")
    void shouldRejectBlankIdempotencyId() throws Exception {
        createWallet("user-i09");

        var body = Map.of(
                "userId", "user-i09",
                "amount", new BigDecimal("100.00"),
                "idempotencyId", "   "
        );

        mockMvc.perform(post("/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}
