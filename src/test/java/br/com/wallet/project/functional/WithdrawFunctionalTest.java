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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Functional tests for withdraw operations.
 *
 * Scenarios covered:
 *  WD-01  Withdraw within balance → balance updated correctly
 *  WD-02  Withdraw exact balance → balance becomes 0.00
 *  WD-03  Withdraw more than balance → rejected, balance unchanged
 *  WD-04  Withdraw from empty wallet → rejected
 *  WD-05  Withdraw zero amount → 400 Bad Request
 *  WD-06  Withdraw negative amount → 400 Bad Request
 *  WD-07  Transaction record has correct before/after snapshot
 */
@DisplayName("Withdraw Operations")
class WithdrawFunctionalTest extends BaseFunctionalTest {

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

    // ── WD-01 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("WD-01: Withdraw 40.00 from balance of 100.00 → remaining balance 60.00")
    void shouldUpdateBalanceAfterWithdraw() throws Exception {
        createWallet("user-wd01");
        deposit("user-wd01", "100.00");

        var body = Map.of(
                "userId", "user-wd01",
                "amount", new BigDecimal("40.00"),
                "idempotencyId", UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = 'user-wd01') AND type = 'WITHDRAW'",
                1, ASYNC_TIMEOUT_MS);

        BigDecimal balance = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-wd01'", BigDecimal.class);
        assertThat(balance).isEqualByComparingTo("60.00");
    }

    // ── WD-02 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("WD-02: Withdraw exact balance amount → balance becomes 0.00")
    void shouldAllowWithdrawOfExactBalance() throws Exception {
        createWallet("user-wd02");
        deposit("user-wd02", "75.00");

        var body = Map.of(
                "userId", "user-wd02",
                "amount", new BigDecimal("75.00"),
                "idempotencyId", UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = 'user-wd02') AND type = 'WITHDRAW'",
                1, ASYNC_TIMEOUT_MS);

        BigDecimal balance = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-wd02'", BigDecimal.class);
        assertThat(balance).isEqualByComparingTo("0.00");
    }

    // ── WD-03 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("WD-03: Withdraw 200.00 from balance of 100.00 → rejected, balance unchanged")
    void shouldRejectWithdrawWhenInsufficientFunds() throws Exception {
        createWallet("user-wd03");
        deposit("user-wd03", "100.00");

        var body = Map.of(
                "userId", "user-wd03",
                "amount", new BigDecimal("200.00"),
                "idempotencyId", UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk()); // async: accepted for processing

        // Give consumer time to attempt processing
        Thread.sleep(3000);

        // No WITHDRAW transaction should be committed
        Integer withdrawCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = 'user-wd03') AND type = 'WITHDRAW'",
                Integer.class);
        assertThat(withdrawCount).isZero();

        // Balance must remain unchanged
        BigDecimal balance = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-wd03'", BigDecimal.class);
        assertThat(balance).isEqualByComparingTo("100.00");
    }

    // ── WD-04 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("WD-04: Withdraw from empty wallet → rejected, balance stays 0.00")
    void shouldRejectWithdrawFromEmptyWallet() throws Exception {
        createWallet("user-wd04");

        var body = Map.of(
                "userId", "user-wd04",
                "amount", new BigDecimal("10.00"),
                "idempotencyId", UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        Thread.sleep(3000);

        Integer withdrawCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = 'user-wd04') AND type = 'WITHDRAW'",
                Integer.class);
        assertThat(withdrawCount).isZero();

        BigDecimal balance = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-wd04'", BigDecimal.class);
        assertThat(balance).isEqualByComparingTo("0.00");
    }

    // ── WD-05 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("WD-05: Withdraw zero amount → 400 Bad Request")
    void shouldRejectZeroAmountWithdraw() throws Exception {
        createWallet("user-wd05");

        var body = Map.of(
                "userId", "user-wd05",
                "amount", BigDecimal.ZERO,
                "idempotencyId", UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ── WD-06 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("WD-06: Withdraw negative amount → 400 Bad Request")
    void shouldRejectNegativeAmountWithdraw() throws Exception {
        createWallet("user-wd06");

        var body = Map.of(
                "userId", "user-wd06",
                "amount", new BigDecimal("-30.00"),
                "idempotencyId", UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ── WD-07 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("WD-07: Transaction record saves balanceBefore=100 and balanceAfter=70 after withdraw of 30")
    void shouldRecordCorrectBalanceSnapshotAfterWithdraw() throws Exception {
        createWallet("user-wd07");
        deposit("user-wd07", "100.00");

        var body = Map.of(
                "userId", "user-wd07",
                "amount", new BigDecimal("30.00"),
                "idempotencyId", UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = 'user-wd07') AND type = 'WITHDRAW'",
                1, ASYNC_TIMEOUT_MS);

        Map<String, Object> tx = jdbcTemplate.queryForMap(
                "SELECT balance_before_transaction, balance_after_transaction FROM transactions " +
                "WHERE wallet_id = (SELECT id FROM wallets WHERE user_id = 'user-wd07') " +
                "AND type = 'WITHDRAW'");

        assertThat((BigDecimal) tx.get("balance_before_transaction"))
                .isEqualByComparingTo("100.00");
        assertThat((BigDecimal) tx.get("balance_after_transaction"))
                .isEqualByComparingTo("70.00");
    }
}
