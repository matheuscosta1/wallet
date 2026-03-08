package br.com.wallet.project.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Functional tests for concurrency control — pessimistic lock + @Version.
 *
 * These tests simulate high-concurrency scenarios to verify the system
 * never ends up with an inconsistent balance due to race conditions.
 *
 * Scenarios covered:
 *  C-01  10 concurrent deposits of 10.00 → final balance is exactly 100.00
 *  C-02  10 concurrent withdraws from 100.00 → final balance is 0.00, no over-draw
 *  C-03  Mixed concurrent deposits and withdrawals → balance remains consistent
 *  C-04  Concurrent transfers between same pair of wallets → sum preserved
 *  C-05  5 concurrent transfers from A to B and 5 from B to A → no deadlock, totals preserved
 *  C-06  100 concurrent deposits of 1.00 → final balance is exactly 100.00 (stress test)
 */
@DisplayName("Concurrency — Pessimistic Lock & @Version")
class ConcurrencyFunctionalTest extends BaseFunctionalTest {

    @Autowired
    private ObjectMapper objectMapper;

    private static final long ASYNC_TIMEOUT_MS = 30_000;

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

    /**
     * Fires n concurrent HTTP requests using a CountDownLatch to maximize simultaneity.
     * Returns the number of successful (HTTP 200) responses.
     */
    private int fireConcurrentRequests(String endpoint, List<Map<String, Object>> bodies)
            throws InterruptedException {
        int n = bodies.size();
        ExecutorService executor = Executors.newFixedThreadPool(n);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);

        for (Map<String, Object> body : bodies) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    String json = new ObjectMapper().writeValueAsString(body);
                    mockMvc.perform(post(endpoint)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json))
                            .andExpect(status().isOk());
                    successCount.incrementAndGet();
                } catch (Exception ignored) { }
            });
        }

        startGate.countDown();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
        return successCount.get();
    }

    // ── C-01 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("C-01: 10 concurrent deposits of 10.00 → final balance exactly 100.00")
    void shouldHandleConcurrentDepositsCorrectly() throws Exception {
        createWallet("user-c01");

        List<Map<String, Object>> bodies = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            bodies.add(Map.of(
                    "userId", "user-c01",
                    "amount", new BigDecimal("10.00"),
                    "idempotencyId", UUID.randomUUID().toString()
            ));
        }

        fireConcurrentRequests("/deposit", bodies);

        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = 'user-c01') AND type = 'DEPOSIT'",
                10, ASYNC_TIMEOUT_MS);

        BigDecimal balance = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-c01'", BigDecimal.class);
        assertThat(balance).isEqualByComparingTo("100.00");
    }

    // ── C-02 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("C-02: 10 concurrent withdraws of 10.00 from balance 100.00 → final balance 0.00, no overdraft")
    void shouldHandleConcurrentWithdrawsWithoutOverdraft() throws Exception {
        createWallet("user-c02");
        deposit("user-c02", "100.00");

        List<Map<String, Object>> bodies = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            bodies.add(Map.of(
                    "userId", "user-c02",
                    "amount", new BigDecimal("10.00"),
                    "idempotencyId", UUID.randomUUID().toString()
            ));
        }

        fireConcurrentRequests("/withdraw", bodies);

        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = 'user-c02') AND type = 'WITHDRAW'",
                10, ASYNC_TIMEOUT_MS);

        BigDecimal balance = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-c02'", BigDecimal.class);

        // Balance must be >= 0 (no overdraft), and equals 0.00 if all 10 were processed
        assertThat(balance).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(balance).isEqualByComparingTo("0.00");
    }

    // ── C-03 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("C-03: 5 concurrent deposits of 20.00 and 5 concurrent withdrawals of 10.00 → balance = 150.00")
    void shouldHandleMixedConcurrentOperations() throws Exception {
        createWallet("user-c03");
        deposit("user-c03", "100.00"); // initial balance to allow withdrawals

        List<Map<String, Object>> depositBodies = new ArrayList<>();
        List<Map<String, Object>> withdrawBodies = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            depositBodies.add(Map.of(
                    "userId", "user-c03",
                    "amount", new BigDecimal("20.00"),
                    "idempotencyId", UUID.randomUUID().toString()
            ));
            withdrawBodies.add(Map.of(
                    "userId", "user-c03",
                    "amount", new BigDecimal("10.00"),
                    "idempotencyId", UUID.randomUUID().toString()
            ));
        }

        // Fire both sets concurrently
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch gate = new CountDownLatch(1);
        for (var body : depositBodies) {
            executor.submit(() -> { try { gate.await(); fireConcurrentRequests("/deposit", List.of(body)); } catch (Exception ignored) {} });
        }
        for (var body : withdrawBodies) {
            executor.submit(() -> { try { gate.await(); fireConcurrentRequests("/withdraw", List.of(body)); } catch (Exception ignored) {} });
        }
        gate.countDown();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        // Wait for all 10 operations to complete (5 deposits + 5 withdraws beyond initial)
        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = 'user-c03')",
                11, ASYNC_TIMEOUT_MS); // 1 initial deposit + 5 deposits + 5 withdrawals

        BigDecimal balance = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-c03'", BigDecimal.class);

        // 100 (initial) + (5 * 20) - (5 * 10) = 150
        assertThat(balance).isEqualByComparingTo("150.00");
        assertThat(balance).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    // ── C-04 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("C-04: 5 concurrent transfers of 10.00 from A to B → total sum conserved (A+B = 100)")
    void shouldConserveTotalBalanceDuringConcurrentTransfers() throws Exception {
        createWallet("user-c04-a");
        createWallet("user-c04-b");
        deposit("user-c04-a", "100.00");

        List<Map<String, Object>> bodies = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            bodies.add(Map.of(
                    "fromUserId", "user-c04-a",
                    "toUserId", "user-c04-b",
                    "amount", new BigDecimal("10.00"),
                    "idempotencyId", UUID.randomUUID().toString()
            ));
        }

        fireConcurrentRequests("/transfer", bodies);

        waitForCondition("SELECT COUNT(*) FROM transfers", 5, ASYNC_TIMEOUT_MS);

        BigDecimal balanceA = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-c04-a'", BigDecimal.class);
        BigDecimal balanceB = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-c04-b'", BigDecimal.class);

        // Total must always be preserved
        BigDecimal total = balanceA.add(balanceB);
        assertThat(total).isEqualByComparingTo("100.00");
        assertThat(balanceA).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(balanceB).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(balanceA).isEqualByComparingTo("50.00");
        assertThat(balanceB).isEqualByComparingTo("50.00");
    }

    // ── C-05 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("C-05: 5 A→B and 5 B→A transfers concurrently → no deadlock, sum preserved")
    void shouldHandleBidirectionalConcurrentTransfersWithoutDeadlock() throws Exception {
        createWallet("user-c05-a");
        createWallet("user-c05-b");
        deposit("user-c05-a", "50.00");
        deposit("user-c05-b", "50.00");

        List<Map<String, Object>> aToB = new ArrayList<>();
        List<Map<String, Object>> bToA = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            aToB.add(Map.of(
                    "fromUserId", "user-c05-a",
                    "toUserId", "user-c05-b",
                    "amount", new BigDecimal("5.00"),
                    "idempotencyId", UUID.randomUUID().toString()
            ));
            bToA.add(Map.of(
                    "fromUserId", "user-c05-b",
                    "toUserId", "user-c05-a",
                    "amount", new BigDecimal("5.00"),
                    "idempotencyId", UUID.randomUUID().toString()
            ));
        }

        List<Map<String, Object>> all = new ArrayList<>(aToB);
        all.addAll(bToA);
        fireConcurrentRequests("/transfer", all);

        waitForCondition("SELECT COUNT(*) FROM transfers", 10, ASYNC_TIMEOUT_MS);

        BigDecimal balanceA = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-c05-a'", BigDecimal.class);
        BigDecimal balanceB = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-c05-b'", BigDecimal.class);

        // Since A→B and B→A are symmetric, totals must be preserved
        BigDecimal total = balanceA.add(balanceB);
        assertThat(total).isEqualByComparingTo("100.00");
        assertThat(balanceA).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(balanceB).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    // ── C-06 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("C-06: Stress test — 100 concurrent deposits of 1.00 → final balance exactly 100.00")
    void stressTestOneHundredConcurrentDeposits() throws Exception {
        createWallet("user-c06");

        List<Map<String, Object>> bodies = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            bodies.add(Map.of(
                    "userId", "user-c06",
                    "amount", new BigDecimal("1.00"),
                    "idempotencyId", UUID.randomUUID().toString()
            ));
        }

        fireConcurrentRequests("/deposit", bodies);

        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = 'user-c06') AND type = 'DEPOSIT'",
                100, ASYNC_TIMEOUT_MS);

        BigDecimal balance = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-c06'", BigDecimal.class);
        assertThat(balance).isEqualByComparingTo("100.00");
    }
}
