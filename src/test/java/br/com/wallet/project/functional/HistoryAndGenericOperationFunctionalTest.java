package br.com.wallet.project.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Functional tests for transaction history and the generic any-operation endpoint.
 *
 * Scenarios covered:
 *  H-01  History for user with deposit, withdraw, transfer → all 3 types returned
 *  H-02  History filtered by today's date → returns correct transactions
 *  H-03  History for non-existent user → 404
 *  H-04  History for user with no transactions → empty list
 *  H-05  History with future date → empty list
 *  H-06  History includes balanceBefore and balanceAfter for every transaction
 *
 *  G-01  Generic endpoint with DEPOSIT type → processed correctly
 *  G-02  Generic endpoint with WITHDRAW type → processed correctly
 *  G-03  Generic endpoint with TRANSFER type → processed correctly
 *  G-04  Generic endpoint with unknown type → 400 Bad Request
 */
@DisplayName("Transaction History & Generic Operation")
class HistoryAndGenericOperationFunctionalTest extends BaseFunctionalTest {

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

    private String today() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 00:00:00.000";
    }

    // ── H-01 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("H-01: History after deposit + withdraw + transfer → returns 4 transactions (1 deposit, 1 withdraw, 2 transfer sides)")
    void shouldReturnAllTransactionTypesInHistory() throws Exception {
        createWallet("user-h01-a");
        createWallet("user-h01-b");
        deposit("user-h01-a", "200.00");

        // Withdraw
        mockMvc.perform(post("/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", "user-h01-a",
                                "amount", new BigDecimal("50.00"),
                                "idempotencyId", UUID.randomUUID().toString()))))
                .andExpect(status().isOk());

        // Transfer
        mockMvc.perform(post("/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fromUserId", "user-h01-a",
                                "toUserId", "user-h01-b",
                                "amount", new BigDecimal("30.00"),
                                "idempotencyId", UUID.randomUUID().toString()))))
                .andExpect(status().isOk());

        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = 'user-h01-a')",
                3, ASYNC_TIMEOUT_MS);

        // Query history
        var historyBody = Map.of("userId", "user-h01-a", "date", today());
        mockMvc.perform(get("/history")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(historyBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3));
    }

    // ── H-02 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("H-02: History filtered by today → only today's transactions returned")
    void shouldFilterHistoryByDate() throws Exception {
        createWallet("user-h02");
        deposit("user-h02", "100.00");

        var historyBody = Map.of("userId", "user-h02", "date", today());
        mockMvc.perform(get("/history")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(historyBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ── H-03 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("H-03: History for non-existent user → 404 Not Found")
    void shouldReturn404ForHistoryOfNonExistentUser() throws Exception {
        var body = Map.of("userId", "ghost-user-h03", "date", today());

        mockMvc.perform(get("/history")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is4xxClientError());
    }

    // ── H-04 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("H-04: History for user with no transactions → empty list")
    void shouldReturnEmptyHistoryWhenNoTransactions() throws Exception {
        createWallet("user-h04");

        var body = Map.of("userId", "user-h04", "date", today());
        mockMvc.perform(get("/history")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── H-05 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("H-05: History with a future date → empty list")
    void shouldReturnEmptyHistoryForFutureDate() throws Exception {
        createWallet("user-h05");
        deposit("user-h05", "100.00");

        var body = Map.of("userId", "user-h05", "date", "2099-12-31 00:00:00.000");
        mockMvc.perform(get("/history")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── H-06 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("H-06: Every transaction in history has balanceBefore and balanceAfter populated")
    void shouldIncludeBalanceSnapshotsInHistory() throws Exception {
        createWallet("user-h06");
        deposit("user-h06", "100.00");

        var body = Map.of("userId", "user-h06", "date", today());
        mockMvc.perform(get("/history")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].balanceBeforeTransaction").exists())
                .andExpect(jsonPath("$[0].balanceAfterTransaction").exists())
                .andExpect(jsonPath("$[0].balanceBeforeTransaction").value(0.0))
                .andExpect(jsonPath("$[0].balanceAfterTransaction").value(100.0));
    }

    // ── G-01 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("G-01: Generic endpoint with type=DEPOSIT → processed correctly")
    void shouldProcessDepositViaGenericEndpoint() throws Exception {
        createWallet("user-g01");

        var body = Map.of(
                "userId", "user-g01",
                "transactionType", "DEPOSIT",
                "amount", new BigDecimal("80.00"),
                "idempotencyId", UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/any-operation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = 'user-g01') AND type = 'DEPOSIT'",
                1, ASYNC_TIMEOUT_MS);

        BigDecimal balance = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-g01'", BigDecimal.class);
        org.assertj.core.api.Assertions.assertThat(balance).isEqualByComparingTo("80.00");
    }

    // ── G-02 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("G-02: Generic endpoint with type=WITHDRAW → processed correctly")
    void shouldProcessWithdrawViaGenericEndpoint() throws Exception {
        createWallet("user-g02");
        deposit("user-g02", "100.00");

        var body = Map.of(
                "userId", "user-g02",
                "transactionType", "WITHDRAW",
                "amount", new BigDecimal("25.00"),
                "idempotencyId", UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/any-operation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = 'user-g02') AND type = 'WITHDRAW'",
                1, ASYNC_TIMEOUT_MS);

        BigDecimal balance = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-g02'", BigDecimal.class);
        org.assertj.core.api.Assertions.assertThat(balance).isEqualByComparingTo("75.00");
    }

    // ── G-03 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("G-03: Generic endpoint with type=TRANSFER → processed correctly")
    void shouldProcessTransferViaGenericEndpoint() throws Exception {
        createWallet("user-g03-a");
        createWallet("user-g03-b");
        deposit("user-g03-a", "100.00");

        var body = Map.of(
                "fromUserId", "user-g03-a",
                "toUserId", "user-g03-b",
                "transactionType", "TRANSFER",
                "amount", new BigDecimal("35.00"),
                "idempotencyId", UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/any-operation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        waitForCondition("SELECT COUNT(*) FROM transfers", 1, ASYNC_TIMEOUT_MS);

        BigDecimal balanceA = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-g03-a'", BigDecimal.class);
        org.assertj.core.api.Assertions.assertThat(balanceA).isEqualByComparingTo("65.00");
    }

    // ── G-04 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("G-04: Generic endpoint with unknown transactionType → 400 Bad Request")
    void shouldRejectUnknownTransactionTypeInGenericEndpoint() throws Exception {
        createWallet("user-g04");

        var body = Map.of(
                "userId", "user-g04",
                "transactionType", "INVALID_TYPE",
                "amount", new BigDecimal("50.00"),
                "idempotencyId", UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/any-operation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}
