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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end scenario tests covering realistic user journeys.
 * These are higher-level tests that combine multiple operations.
 *
 * Scenarios covered:
 *  E-01  Full user lifecycle: create → deposit → withdraw → transfer → check history
 *  E-02  Merchant payment scenario: user pays merchant, both balances updated correctly
 *  E-03  Salary scenario: employer deposits to multiple employees simultaneously
 *  E-04  Refund scenario: user pays, then receives refund — final balance equals original
 *  E-05  Overdraft attempt chain: deposit → partial withdraw → attempt overdraft → balance safe
 */
@DisplayName("End-to-End Scenarios")
class EndToEndScenarioFunctionalTest extends BaseFunctionalTest {

    @Autowired
    private ObjectMapper objectMapper;

    private static final long ASYNC_TIMEOUT_MS = 15_000;

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

    private void withdraw(String userId, String amount) throws Exception {
        var body = Map.of(
                "userId", userId,
                "amount", new BigDecimal(amount),
                "idempotencyId", UUID.randomUUID().toString()
        );
        mockMvc.perform(post("/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    private void transfer(String from, String to, String amount) throws Exception {
        var body = Map.of(
                "fromUserId", from,
                "toUserId", to,
                "amount", new BigDecimal(amount),
                "idempotencyId", UUID.randomUUID().toString()
        );
        mockMvc.perform(post("/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    private BigDecimal getBalance(String userId) {
        return jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = '" + userId + "'", BigDecimal.class);
    }

    private String today() {
        return java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 00:00:00.000";
    }

    // ── E-01 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("E-01: Full lifecycle — create, deposit, withdraw, transfer, check history")
    void fullUserLifecycle() throws Exception {
        createWallet("user-e01-alice");
        createWallet("user-e01-bob");

        // Alice deposits 500
        deposit("user-e01-alice", "500.00");
        assertThat(getBalance("user-e01-alice")).isEqualByComparingTo("500.00");

        // Alice withdraws 100
        withdraw("user-e01-alice", "100.00");
        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = 'user-e01-alice') AND type = 'WITHDRAW'",
                1, ASYNC_TIMEOUT_MS);
        assertThat(getBalance("user-e01-alice")).isEqualByComparingTo("400.00");

        // Alice transfers 150 to Bob
        transfer("user-e01-alice", "user-e01-bob", "150.00");
        waitForCondition("SELECT COUNT(*) FROM transfers", 1, ASYNC_TIMEOUT_MS);
        assertThat(getBalance("user-e01-alice")).isEqualByComparingTo("250.00");
        assertThat(getBalance("user-e01-bob")).isEqualByComparingTo("150.00");

        // Check Alice's full history: 1 deposit + 1 withdraw + 1 transfer-out = 3 transactions
        var historyBody = Map.of("userId", "user-e01-alice", "date", today());
        mockMvc.perform(get("/history")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(historyBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    // ── E-02 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("E-02: Merchant payment — user pays 30.00, merchant receives exactly 30.00")
    void merchantPaymentScenario() throws Exception {
        createWallet("user-e02-customer");
        createWallet("user-e02-merchant");
        deposit("user-e02-customer", "100.00");

        transfer("user-e02-customer", "user-e02-merchant", "30.00");
        waitForCondition("SELECT COUNT(*) FROM transfers", 1, ASYNC_TIMEOUT_MS);

        assertThat(getBalance("user-e02-customer")).isEqualByComparingTo("70.00");
        assertThat(getBalance("user-e02-merchant")).isEqualByComparingTo("30.00");

        // Verify transfer record exists and links both transactions
        Integer transferCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transfers", Integer.class);
        assertThat(transferCount).isEqualTo(1);
    }

    // ── E-03 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("E-03: Employer deposits salary to 3 employees simultaneously")
    void salaryPaymentToMultipleEmployees() throws Exception {
        createWallet("employer-e03");
        createWallet("employee-e03-1");
        createWallet("employee-e03-2");
        createWallet("employee-e03-3");

        deposit("employer-e03", "10000.00");

        // Pay three employees
        transfer("employer-e03", "employee-e03-1", "3000.00");
        transfer("employer-e03", "employee-e03-2", "3500.00");
        transfer("employer-e03", "employee-e03-3", "2000.00");

        waitForCondition("SELECT COUNT(*) FROM transfers", 3, ASYNC_TIMEOUT_MS);

        assertThat(getBalance("employer-e03")).isEqualByComparingTo("1500.00");
        assertThat(getBalance("employee-e03-1")).isEqualByComparingTo("3000.00");
        assertThat(getBalance("employee-e03-2")).isEqualByComparingTo("3500.00");
        assertThat(getBalance("employee-e03-3")).isEqualByComparingTo("2000.00");

        // Verify total is preserved
        BigDecimal total = getBalance("employer-e03")
                .add(getBalance("employee-e03-1"))
                .add(getBalance("employee-e03-2"))
                .add(getBalance("employee-e03-3"));
        assertThat(total).isEqualByComparingTo("10000.00");
    }

    // ── E-04 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("E-04: Refund scenario — user pays 50, receives full refund, ends with original balance")
    void refundScenario() throws Exception {
        createWallet("user-e04-buyer");
        createWallet("user-e04-seller");
        deposit("user-e04-buyer", "200.00");

        // Buyer pays seller
        transfer("user-e04-buyer", "user-e04-seller", "50.00");
        waitForCondition("SELECT COUNT(*) FROM transfers", 1, ASYNC_TIMEOUT_MS);

        assertThat(getBalance("user-e04-buyer")).isEqualByComparingTo("150.00");

        // Seller refunds buyer
        transfer("user-e04-seller", "user-e04-buyer", "50.00");
        waitForCondition("SELECT COUNT(*) FROM transfers", 2, ASYNC_TIMEOUT_MS);

        // Buyer should be back to original balance
        assertThat(getBalance("user-e04-buyer")).isEqualByComparingTo("200.00");
        assertThat(getBalance("user-e04-seller")).isEqualByComparingTo("0.00");
    }

    // ── E-05 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("E-05: Overdraft attempt chain — partial withdraw then overdraft attempt → balance never negative")
    void overdraftAttemptChain() throws Exception {
        createWallet("user-e05");
        deposit("user-e05", "100.00");

        // Valid withdraw: 100 → 60
        withdraw("user-e05", "40.00");
        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = 'user-e05') AND type = 'WITHDRAW'",
                1, ASYNC_TIMEOUT_MS);
        assertThat(getBalance("user-e05")).isEqualByComparingTo("60.00");

        // Overdraft attempt: try to withdraw 80 from 60
        var overdraftBody = Map.of(
                "userId", "user-e05",
                "amount", new BigDecimal("80.00"),
                "idempotencyId", UUID.randomUUID().toString()
        );
        mockMvc.perform(post("/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overdraftBody)))
                .andExpect(status().isOk()); // async accepted

        Thread.sleep(3000);

        // Balance should remain at 60 — overdraft not applied
        assertThat(getBalance("user-e05")).isEqualByComparingTo("60.00");
        assertThat(getBalance("user-e05")).isGreaterThanOrEqualTo(BigDecimal.ZERO);

        // Only 1 WITHDRAW transaction should exist (the successful one)
        Integer withdrawCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = 'user-e05') AND type = 'WITHDRAW'",
                Integer.class);
        assertThat(withdrawCount).isEqualTo(1);
    }
}
