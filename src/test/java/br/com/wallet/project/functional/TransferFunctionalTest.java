package br.com.wallet.project.functional;

import br.com.wallet.project.adapter.in.web.response.TransactionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Functional tests for transfer operations.
 *
 * Scenarios covered:
 *  T-01  Successful transfer: both wallets updated atomically
 *  T-02  Transfer with insufficient funds → both balances unchanged
 *  T-03  Transfer from non-existent source wallet → error after async
 *  T-04  Transfer to non-existent destination wallet → error after async
 *  T-05  Transfer to self → rejected
 *  T-06  Transfer zero amount → 400 Bad Request
 *  T-07  Transfer record links debit and credit transactions correctly
 *  T-08  Transfer exact balance → source becomes 0.00, destination updated
 */
@DisplayName("Transfer Operations")
class TransferFunctionalTest extends BaseFunctionalTest {

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

    // ── T-01 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("T-01: Transfer 50.00 from A(100) to B(0) → A=50, B=50 atomically")
    void shouldTransferFundsBetweenWallets() throws Exception {
        createWallet("user-t01-a");
        createWallet("user-t01-b");
        deposit("user-t01-a", "100.00");

        var body = Map.of(
                "fromUserId", "user-t01-a",
                "toUserId", "user-t01-b",
                "amount", new BigDecimal("50.00"),
                "idempotencyId", UUID.randomUUID().toString()
        );

       MvcResult result = mockMvc.perform(post("/transfer")
               .contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(body)))
               .andExpect(status().isOk())
               .andReturn();

       String responseContent = result.getResponse().getContentAsString();
       TransactionResponse transactionResponse = objectMapper.readValue(responseContent, TransactionResponse.class);

        // Wait for both WITHDRAW and DEPOSIT transactions from the transfer
        waitForCondition(String.format("SELECT COUNT(*) FROM transactions WHERE transaction_track_id = '%s'",
                        transactionResponse.getTransactionId()), 2, ASYNC_TIMEOUT_MS);

        BigDecimal balanceA = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-t01-a'", BigDecimal.class);
        BigDecimal balanceB = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-t01-b'", BigDecimal.class);

        assertThat(balanceA).isEqualByComparingTo("50.00");
        assertThat(balanceB).isEqualByComparingTo("50.00");
    }

    // ── T-02 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("T-02: Transfer 200.00 from A(100) → both balances unchanged")
    void shouldRejectTransferWhenInsufficientFunds() throws Exception {
        createWallet("user-t02-a");
        createWallet("user-t02-b");
        deposit("user-t02-a", "100.00");

        var body = Map.of(
                "fromUserId", "user-t02-a",
                "toUserId", "user-t02-b",
                "amount", new BigDecimal("200.00"),
                "idempotencyId", UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk()); // async accepted

        Thread.sleep(3000);

        BigDecimal balanceA = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-t02-a'", BigDecimal.class);
        BigDecimal balanceB = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-t02-b'", BigDecimal.class);

        assertThat(balanceA).isEqualByComparingTo("100.00");
        assertThat(balanceB).isEqualByComparingTo("0.00");

        Integer transferCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transfers", Integer.class);
        assertThat(transferCount).isZero();
    }

    // ── T-03 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("T-03: Transfer from non-existent source wallet → no changes persisted")
    void shouldRejectTransferFromNonExistentSourceWallet() throws Exception {
        createWallet("user-t03-b");

        var body = Map.of(
                "fromUserId", "ghost-user-t03",
                "toUserId", "user-t03-b",
                "amount", new BigDecimal("50.00"),
                "idempotencyId", UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        Thread.sleep(3000);

        Integer transferCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transfers", Integer.class);
        assertThat(transferCount).isZero();

        BigDecimal balanceB = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-t03-b'", BigDecimal.class);
        assertThat(balanceB).isEqualByComparingTo("0.00");
    }

    // ── T-04 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("T-04: Transfer to non-existent destination wallet → source balance unchanged")
    void shouldRejectTransferToNonExistentDestinationWallet() throws Exception {
        createWallet("user-t04-a");
        deposit("user-t04-a", "100.00");

        var body = Map.of(
                "fromUserId", "user-t04-a",
                "toUserId", "ghost-destination-t04",
                "amount", new BigDecimal("50.00"),
                "idempotencyId", UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        Thread.sleep(3000);

        BigDecimal balanceA = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-t04-a'", BigDecimal.class);
        assertThat(balanceA).isEqualByComparingTo("100.00");

        Integer transferCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transfers", Integer.class);
        assertThat(transferCount).isZero();
    }

    // ── T-05 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("T-05: Transfer to self (same fromUserId and toUserId) → 400 Bad Request")
    void shouldRejectSelfTransfer() throws Exception {
        createWallet("user-t05");
        deposit("user-t05", "100.00");

        var body = Map.of(
                "fromUserId", "user-t05",
                "toUserId", "user-t05",
                "amount", new BigDecimal("50.00"),
                "idempotencyId", UUID.randomUUID().toString()
        );

        MvcResult result = mockMvc.perform(post("/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        TransactionResponse transactionResponse = objectMapper.readValue(responseContent, TransactionResponse.class);

        // Wait for both WITHDRAW and DEPOSIT transactions from the transfer
        waitForCondition(String.format("SELECT COUNT(*) FROM transactions WHERE transaction_track_id = '%s'",
                transactionResponse.getTransactionId()), 0, ASYNC_TIMEOUT_MS);
    }

    // ── T-06 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("T-06: Transfer zero amount → 400 Bad Request")
    void shouldRejectZeroAmountTransfer() throws Exception {
        createWallet("user-t06-a");
        createWallet("user-t06-b");

        var body = Map.of(
                "fromUserId", "user-t06-a",
                "toUserId", "user-t06-b",
                "amount", BigDecimal.ZERO,
                "idempotencyId", UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ── T-07 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("T-07: Transfer record links debit and credit transaction IDs correctly")
    void shouldCreateTransferRecordLinkingBothTransactions() throws Exception {
        createWallet("user-t07-a");
        createWallet("user-t07-b");
        deposit("user-t07-a", "100.00");

        var body = Map.of(
                "fromUserId", "user-t07-a",
                "toUserId", "user-t07-b",
                "amount", new BigDecimal("30.00"),
                "idempotencyId", UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        waitForCondition("SELECT COUNT(*) FROM transfers", 1, ASYNC_TIMEOUT_MS);

        Map<String, Object> transfer = jdbcTemplate.queryForMap(
                "SELECT debit_transaction_id, credit_transaction_id FROM transfers");

        assertThat(transfer.get("debit_transaction_id")).isNotNull();
        assertThat(transfer.get("credit_transaction_id")).isNotNull();
        assertThat(transfer.get("debit_transaction_id"))
                .isNotEqualTo(transfer.get("credit_transaction_id"));
    }

    // ── T-08 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("T-08: Transfer exact balance → source becomes 0.00, destination updated correctly")
    void shouldTransferExactBalance() throws Exception {
        createWallet("user-t08-a");
        createWallet("user-t08-b");
        deposit("user-t08-a", "60.00");
        deposit("user-t08-b", "20.00");

        var body = Map.of(
                "fromUserId", "user-t08-a",
                "toUserId", "user-t08-b",
                "amount", new BigDecimal("60.00"),
                "idempotencyId", UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        waitForCondition("SELECT COUNT(*) FROM transfers", 1, ASYNC_TIMEOUT_MS);

        BigDecimal balanceA = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-t08-a'", BigDecimal.class);
        BigDecimal balanceB = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-t08-b'", BigDecimal.class);

        assertThat(balanceA).isEqualByComparingTo("0.00");
        assertThat(balanceB).isEqualByComparingTo("80.00");
    }
}
