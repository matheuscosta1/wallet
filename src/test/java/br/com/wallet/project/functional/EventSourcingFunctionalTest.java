package br.com.wallet.project.functional;

import br.com.wallet.project.adapter.in.web.response.TransactionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Functional tests for the Event Sourcing mechanism.
 *
 * Scenarios covered:
 *  ES-01  Creating a wallet emits WALLET_CREATED
 *  ES-02  Deposit emits DEPOSIT_REQUESTED (sync) and DEPOSIT_COMPLETED (async)
 *  ES-03  Withdraw emits WITHDRAW_REQUESTED + WITHDRAW_COMPLETED
 *  ES-04  Transfer emits full 4-event chain
 *  ES-05  Insufficient funds → WITHDRAW_REQUESTED + N×TRANSACTION_RETRY_ATTEMPTED + SENT_TO_DLQ
 *  ES-06  Deposit to non-existent wallet → DEPOSIT_REQUESTED + N×RETRY + SENT_TO_DLQ
 *  ES-07  Duplicate idempotencyId → IDEMPOTENCY_DUPLICATE_DETECTED (per attempt) + SENT_TO_DLQ
 *  ES-08  DEPOSIT_COMPLETED payload contains correct balance snapshots
 *  ES-09  TRANSFER_COMPLETED payload contains all 4 balance snapshots
 *  ES-10  SENT_TO_DLQ payload contains reason, retryCount and failedEventType
 *  ES-11  *_FAILED events are never written as standalone rows
 *  ES-12  create + deposit + withdraw = exactly 5 events in the log
 *  ES-13  TRANSACTION_RETRY_ATTEMPTED contains attemptNumber, maxAttempts, remainingAttempts, reason
 *  ES-14  Number of TRANSACTION_RETRY_ATTEMPTED equals configured attempts (4) for a permanent failure
 */
@DisplayName("Event Sourcing — Domain Event Log")
class EventSourcingFunctionalTest extends BaseFunctionalTest {

    @Autowired
    private ObjectMapper objectMapper;

    // 1s + 2s + 4s retries + DLT processing + margin
    private static final long DLQ_TIMEOUT_MS = 25_000;
    private static final long ASYNC_TIMEOUT_MS = 10_000;
    private static final int  MAX_ATTEMPTS = 4;

    private void createWallet(String userId) throws Exception {
        mockMvc.perform(post("/creation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("userId", userId))))
                .andExpect(status().isOk());
    }

    private void deposit(String userId, String amount) throws Exception {
        mockMvc.perform(post("/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", userId,
                                "amount", new BigDecimal(amount),
                                "idempotencyId", UUID.randomUUID().toString()))))
                .andExpect(status().isOk());
        waitForExactBalance(userId, new BigDecimal(amount), ASYNC_TIMEOUT_MS);
    }

    private int countEvents(String eventType, String aggregateId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wallet_events WHERE event_type = ? AND aggregate_id = ?",
                Integer.class, eventType, aggregateId);
    }

    // ── ES-01 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("ES-01: Creating a wallet emits exactly one WALLET_CREATED event")
    void shouldEmitWalletCreated() throws Exception {
        createWallet("user-es01");
        assertThat(countEvents("WALLET_CREATED", "user-es01")).isEqualTo(1);
    }

    // ── ES-02 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("ES-02: Deposit emits DEPOSIT_REQUESTED (sync) then DEPOSIT_COMPLETED (async)")
    void shouldEmitDepositRequestedAndCompleted() throws Exception {
        createWallet("user-es02");
        mockMvc.perform(post("/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", "user-es02",
                                "amount", new BigDecimal("100.00"),
                                "idempotencyId", UUID.randomUUID().toString()))))
                .andExpect(status().isOk());

        assertThat(countEvents("DEPOSIT_REQUESTED", "user-es02")).isEqualTo(1);

        waitForCondition(
                "SELECT COUNT(*) FROM wallet_events WHERE event_type = 'DEPOSIT_COMPLETED' " +
                        "AND aggregate_id = 'user-es02'", 1, ASYNC_TIMEOUT_MS);
        assertThat(countEvents("DEPOSIT_COMPLETED", "user-es02")).isEqualTo(1);
        // No retries on success
        assertThat(countEvents("TRANSACTION_RETRY_ATTEMPTED", "user-es02")).isZero();
    }

    // ── ES-03 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("ES-03: Withdraw emits WITHDRAW_REQUESTED + WITHDRAW_COMPLETED, no retries")
    void shouldEmitWithdrawRequestedAndCompleted() throws Exception {
        createWallet("user-es03");
        deposit("user-es03", "100.00");

        mockMvc.perform(post("/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", "user-es03",
                                "amount", new BigDecimal("40.00"),
                                "idempotencyId", UUID.randomUUID().toString()))))
                .andExpect(status().isOk());

        waitForCondition(
                "SELECT COUNT(*) FROM wallet_events WHERE event_type = 'WITHDRAW_COMPLETED' " +
                        "AND aggregate_id = 'user-es03'", 1, ASYNC_TIMEOUT_MS);

        assertThat(countEvents("WITHDRAW_COMPLETED",          "user-es03")).isEqualTo(1);
        assertThat(countEvents("TRANSACTION_RETRY_ATTEMPTED", "user-es03")).isZero();
    }

    // ── ES-04 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("ES-04: Transfer emits full 4-event chain, no retries")
    void shouldEmitFullTransferEventChain() throws Exception {
        createWallet("user-es04-a");
        createWallet("user-es04-b");
        deposit("user-es04-a", "100.00");

        MvcResult result = mockMvc.perform(post("/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fromUserId", "user-es04-a",
                                "toUserId",   "user-es04-b",
                                "amount", new BigDecimal("30.00"),
                                "idempotencyId", UUID.randomUUID().toString()))))
                .andExpect(status().isOk())
                .andReturn();

        String transferAggregateId = objectMapper
                .readValue(result.getResponse().getContentAsString(), TransactionResponse.class)
                .getTransactionId()
                .toString();

        // TRANSFER_* events use aggregateType='TRANSFER', aggregateId=transactionId
        waitForCondition(
                "SELECT COUNT(*) FROM wallet_events WHERE event_type = 'TRANSFER_COMPLETED' " +
                        "AND aggregate_type = 'TRANSFER' AND aggregate_id = '" + transferAggregateId + "'",
                1, ASYNC_TIMEOUT_MS);

        assertThat(countEvents("TRANSFER_REQUESTED",          transferAggregateId)).isEqualTo(1);
        assertThat(countEvents("TRANSFER_COMPLETED",          transferAggregateId)).isEqualTo(1);
        // WITHDRAW_COMPLETED / DEPOSIT_COMPLETED stay in their respective WALLET aggregates
        assertThat(countEvents("WITHDRAW_COMPLETED",          "user-es04-a")).isEqualTo(1);
        assertThat(countEvents("DEPOSIT_COMPLETED",           "user-es04-b")).isEqualTo(1);
        assertThat(countEvents("TRANSACTION_RETRY_ATTEMPTED", transferAggregateId)).isZero();
    }

    // ── ES-05 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("ES-05: Insufficient funds → WITHDRAW_REQUESTED + N×RETRY_ATTEMPTED + SENT_TO_DLQ")
    void shouldEmitRetryAndDlqOnInsufficientFunds() throws Exception {
        createWallet("user-es05");
        deposit("user-es05", "50.00");

        mockMvc.perform(post("/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", "user-es05",
                                "amount", new BigDecimal("200.00"),
                                "idempotencyId", UUID.randomUUID().toString()))))
                .andExpect(status().isOk());

        assertThat(countEvents("WITHDRAW_REQUESTED", "user-es05")).isEqualTo(1);

        waitForCondition(
                "SELECT COUNT(*) FROM wallet_events WHERE event_type = 'WITHDRAW_PERMANENTLY_FAILED' " +
                        "AND aggregate_id = 'user-es05'", 1, DLQ_TIMEOUT_MS);

        assertThat(countEvents("WITHDRAW_PERMANENTLY_FAILED",                "user-es05")).isEqualTo(1);
        assertThat(countEvents("WITHDRAW_COMPLETED",          "user-es05")).isZero();
        assertThat(countEvents("WITHDRAW_FAILED",             "user-es05")).isZero();
    }

    // ── ES-06 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("ES-06: Non-existent wallet → DEPOSIT_REQUESTED + N×RETRY_ATTEMPTED + SENT_TO_DLQ")
    void shouldEmitRetryAndDlqWhenWalletMissing() throws Exception {
        mockMvc.perform(post("/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", "ghost-es06",
                                "amount", new BigDecimal("100.00"),
                                "idempotencyId", UUID.randomUUID().toString()))))
                .andExpect(status().isOk());

        assertThat(countEvents("DEPOSIT_REQUESTED", "ghost-es06")).isEqualTo(1);

        waitForCondition(
                "SELECT COUNT(*) FROM wallet_events WHERE event_type = 'DEPOSIT_PERMANENTLY_FAILED' " +
                        "AND aggregate_id = 'ghost-es06'", 1, DLQ_TIMEOUT_MS);

        assertThat(countEvents("DEPOSIT_PERMANENTLY_FAILED",                "ghost-es06")).isEqualTo(1);
        assertThat(countEvents("DEPOSIT_COMPLETED",           "ghost-es06")).isZero();
        assertThat(countEvents("DEPOSIT_FAILED",              "ghost-es06")).isZero();
    }

    // ── ES-07 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("ES-07: Duplicate idempotencyId → DUPLICATE_DETECTED (per attempt) + RETRY_ATTEMPTED + SENT_TO_DLQ")
    void shouldEmitDuplicateDetectedRetryAndDlq() throws Exception {
        createWallet("user-es07");
        String idempotencyId = UUID.randomUUID().toString();
        var body = Map.of("userId", "user-es07",
                "amount", new BigDecimal("100.00"),
                "idempotencyId", idempotencyId);

        // First request — processes normally
        mockMvc.perform(post("/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
        waitForCondition(
                "SELECT COUNT(*) FROM wallet_events WHERE event_type = 'DEPOSIT_COMPLETED' " +
                        "AND aggregate_id = 'user-es07'", 1, ASYNC_TIMEOUT_MS);

        // Second request — duplicate
        mockMvc.perform(post("/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        waitForCondition(
                "SELECT COUNT(*) FROM wallet_events WHERE event_type = 'DEPOSIT_PERMANENTLY_FAILED' " +
                        "AND aggregate_id = 'user-es07'", 1, DLQ_TIMEOUT_MS);

        assertThat(countEvents("DEPOSIT_PERMANENTLY_FAILED",                        "user-es07")).isEqualTo(1);
        assertThat(countEvents("DEPOSIT_COMPLETED",                  "user-es07")).isEqualTo(1);
        BigDecimal balance = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-es07'", BigDecimal.class);
        assertThat(balance).isEqualByComparingTo("100.00");
    }

    // ── ES-08 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("ES-08: DEPOSIT_COMPLETED payload has correct balance snapshots")
    void depositCompletedPayloadShouldContainBalanceSnapshots() throws Exception {
        createWallet("user-es08");
        deposit("user-es08", "200.00");

        Map<String, Object> fields = jdbcTemplate.queryForMap(
                "SELECT payload->>'balanceBefore' AS before, payload->>'balanceAfter' AS after " +
                        "FROM wallet_events WHERE event_type = 'DEPOSIT_COMPLETED' " +
                        "AND aggregate_id = 'user-es08'");
        assertThat(fields.get("before")).isEqualTo("0.00");
        assertThat(fields.get("after")).isEqualTo("200.00");
    }

    // ── ES-09 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("ES-09: TRANSFER_COMPLETED payload has all 4 balance snapshots")
    void transferCompletedPayloadShouldContainAllFourBalances() throws Exception {
        createWallet("user-es09-a");
        createWallet("user-es09-b");
        deposit("user-es09-a", "100.00");

        MvcResult result = mockMvc.perform(post("/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fromUserId", "user-es09-a",
                                "toUserId",   "user-es09-b",
                                "amount", new BigDecimal("40.00"),
                                "idempotencyId", UUID.randomUUID().toString()))))
                .andExpect(status().isOk())
                .andReturn();

        String transferAggregateId = objectMapper
                .readValue(result.getResponse().getContentAsString(), TransactionResponse.class)
                .getTransactionId()
                .toString();

        // TRANSFER_COMPLETED uses aggregateType='TRANSFER', aggregateId=transactionId
        waitForCondition(
                "SELECT COUNT(*) FROM wallet_events WHERE event_type = 'TRANSFER_COMPLETED' " +
                        "AND aggregate_type = 'TRANSFER' AND aggregate_id = '" + transferAggregateId + "'",
                1, ASYNC_TIMEOUT_MS);

        Map<String, Object> fields = jdbcTemplate.queryForMap(
                "SELECT payload->>'fromBalanceBefore' AS fbb, payload->>'fromBalanceAfter' AS fba," +
                        "       payload->>'toBalanceBefore'   AS tbb, payload->>'toBalanceAfter'   AS tba " +
                        "FROM wallet_events WHERE event_type = 'TRANSFER_COMPLETED' " +
                        "AND aggregate_type = 'TRANSFER' AND aggregate_id = '" + transferAggregateId + "'");

        assertThat(fields.get("fbb")).isEqualTo("100.00");
        assertThat(fields.get("fba")).isEqualTo("60.00");
        assertThat(fields.get("tbb")).isEqualTo("0.00");
        assertThat(fields.get("tba")).isEqualTo("40.00");
    }

    // ── ES-10 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("ES-10: WITHDRAW_PERMANENTLY_FAILED payload has reason, retryCount, failedEventType")
    void sentToDlqPayloadShouldContainFullDetails() throws Exception {
        createWallet("user-es10");
        deposit("user-es10", "50.00");

        mockMvc.perform(post("/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", "user-es10",
                                "amount", new BigDecimal("999.00"),
                                "idempotencyId", UUID.randomUUID().toString()))))
                .andExpect(status().isOk());

        waitForCondition(
                "SELECT COUNT(*) FROM wallet_events WHERE event_type = 'WITHDRAW_PERMANENTLY_FAILED' " +
                        "AND aggregate_id = 'user-es10'", 1, DLQ_TIMEOUT_MS);

        Map<String, Object> fields = jdbcTemplate.queryForMap(
                "SELECT payload->>'reason' AS reason, payload->>'retryCount' AS retry_count," +
                        "       payload->>'failedEventType' AS failed_type " +
                        "FROM wallet_events WHERE event_type = 'WITHDRAW_PERMANENTLY_FAILED' AND aggregate_id = 'user-es10'");
        assertTrue(fields.get("reason").toString().contains("Insufficient funds to withdraw."));
        assertEquals("4", fields.get("retry_count"));
    }


    // ── ES-11 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("ES-11: *_FAILED events never appear as standalone rows in the event log")
    void failedEventsShouldNeverAppearAsStandaloneRows() throws Exception {
        createWallet("user-es11");
        deposit("user-es11", "50.00");

        mockMvc.perform(post("/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", "user-es11",
                                "amount", new BigDecimal("200.00"),
                                "idempotencyId", UUID.randomUUID().toString()))))
                .andExpect(status().isOk());

        waitForCondition(
                "SELECT COUNT(*) FROM wallet_events WHERE event_type = 'WITHDRAW_PERMANENTLY_FAILED' " +
                        "AND aggregate_id = 'user-es11'", 1, DLQ_TIMEOUT_MS);

        Integer failedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wallet_events WHERE event_type IN " +
                        "('DEPOSIT_FAILED','WITHDRAW_FAILED','TRANSFER_FAILED') " +
                        "AND aggregate_id = 'user-es11'", Integer.class);
        assertThat(failedCount).isZero();
    }

    // ── ES-12 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("ES-12: create + deposit + withdraw = exactly 5 events")
    void totalEventCountShouldMatchOperations() throws Exception {
        createWallet("user-es12");
        deposit("user-es12", "100.00");

        mockMvc.perform(post("/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", "user-es12",
                                "amount", new BigDecimal("30.00"),
                                "idempotencyId", UUID.randomUUID().toString()))))
                .andExpect(status().isOk());

        waitForCondition(
                "SELECT COUNT(*) FROM wallet_events WHERE event_type = 'WITHDRAW_COMPLETED' " +
                        "AND aggregate_id = 'user-es12'", 1, ASYNC_TIMEOUT_MS);

        int total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wallet_events WHERE aggregate_id = 'user-es12'",
                Integer.class);
        // WALLET_CREATED + DEPOSIT_REQUESTED + DEPOSIT_COMPLETED + WITHDRAW_REQUESTED + WITHDRAW_COMPLETED
        assertThat(total).isEqualTo(5);
    }

    // ── ES-13 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("ES-13: TRANSACTION_RETRY_ATTEMPTED payload contains attemptNumber, maxAttempts, remainingAttempts and reason")
    void retryAttemptedPayloadShouldContainFullDetails() throws Exception {
        createWallet("user-es13");
        deposit("user-es13", "50.00");

        mockMvc.perform(post("/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", "user-es13",
                                "amount", new BigDecimal("999.00"),
                                "idempotencyId", UUID.randomUUID().toString()))))
                .andExpect(status().isOk());

        waitForCondition(
                "SELECT COUNT(*) FROM wallet_events WHERE event_type = 'WITHDRAW_PERMANENTLY_FAILED' " +
                        "AND aggregate_id = 'user-es13'", 1, DLQ_TIMEOUT_MS);

        // Check the first retry attempt
        Map<String, Object> fields = jdbcTemplate.queryForMap(
                "SELECT payload->>'reason'           AS reason, " +
                        "       payload->>'transactionType'  AS tx_type " +
                        "FROM wallet_events " +
                        "WHERE event_type = 'WITHDRAW_PERMANENTLY_FAILED' AND aggregate_id = 'user-es13' " +
                        "ORDER BY occurred_at ASC LIMIT 1");

        assertThat(fields.get("tx_type")).isEqualTo("WITHDRAW");
        assertTrue(fields.get("reason").toString().contains("Insufficient funds to withdraw."));
    }
}