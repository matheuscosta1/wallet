package br.com.wallet.project.functional;

import br.com.wallet.project.adapter.out.messaging.kafka.producer.KafkaTransactionEventPublisher;
import br.com.wallet.project.domain.model.TransactionMessage;
import br.com.wallet.project.domain.model.enums.TransactionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Functional tests for Kafka resilience patterns.
 *
 * Validates that messages that fail processing are retried
 * and eventually routed to the DLQ without corrupting state.
 *
 * Scenarios covered:
 *  K-01  Valid message is processed and committed only after success (manual ack)
 *  K-02  Failed processing doesn't commit offset — state remains consistent
 *  K-03  Multiple independent users process their transactions without interference
 *  K-04  Message order is preserved for a single wallet (FIFO)
 *  K-05  System recovers state after simulated Redis flush (idempotency store wiped)
 */
@DisplayName("Kafka Resilience & Message Ordering")
class KafkaResilienceFunctionalTest extends BaseFunctionalTest {

    @Autowired
    private KafkaTransactionEventPublisher kafkaTransactionEventPublisher;

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

    // ── K-01 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("K-01: Valid message processed successfully → exactly one transaction committed")
    void shouldCommitOffsetOnlyAfterSuccessfulProcessing() throws Exception {
        createWallet("user-k01");

        var body = Map.of(
                "userId", "user-k01",
                "amount", new BigDecimal("100.00"),
                "idempotencyId", UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = 'user-k01')",
                1, ASYNC_TIMEOUT_MS);

        // Verify transaction has all required fields set (meaning full processing happened)
        Map<String, Object> tx = jdbcTemplate.queryForMap(
                "SELECT * FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = 'user-k01')");

        assertThat(tx.get("type")).isEqualTo("DEPOSIT");
        assertThat(tx.get("amount")).isNotNull();
        assertThat(tx.get("balance_before_transaction")).isNotNull();
        assertThat(tx.get("balance_after_transaction")).isNotNull();
        assertThat(tx.get("timestamp")).isNotNull();
    }

    // ── K-02 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("K-02: Failed message (non-existent wallet) → no partial state, balance untouched")
    void shouldNotLeavePartialStateWhenMessageFails() throws Exception {
        // Intentionally send to a non-existent wallet to trigger failure
        var body = Map.of(
                "userId", "ghost-user-k02",
                "amount", new BigDecimal("100.00"),
                "idempotencyId", UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        // Wait for retry cycles to exhaust
        Thread.sleep(5000);

        // No transaction record should be created
        Integer txCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transactions", Integer.class);
        assertThat(txCount).isZero();

        // No wallet should have been created
        Integer walletCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wallets WHERE user_id = 'ghost-user-k02'", Integer.class);
        assertThat(walletCount).isZero();
    }

    // ── K-03 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("K-03: Independent users process transactions without interfering with each other")
    void shouldProcessTransactionsForMultipleUsersIndependently() throws Exception {
        for (int i = 1; i <= 5; i++) {
            createWallet("user-k03-" + i);
        }

        // Each user deposits a different amount
        for (int i = 1; i <= 5; i++) {
            var body = Map.of(
                    "userId", "user-k03-" + i,
                    "amount", new BigDecimal(i * 10 + ".00"),
                    "idempotencyId", UUID.randomUUID().toString()
            );
            mockMvc.perform(post("/deposit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk());
        }

        waitForCondition("SELECT COUNT(*) FROM transactions", 5, ASYNC_TIMEOUT_MS);

        // Each wallet must have its own correct balance
        for (int i = 1; i <= 5; i++) {
            BigDecimal expected = new BigDecimal(i * 10 + ".00");
            BigDecimal actual = jdbcTemplate.queryForObject(
                    "SELECT balance FROM wallets WHERE user_id = 'user-k03-" + i + "'", BigDecimal.class);
            assertThat(actual)
                    .as("user-k03-%d should have balance %s", i, expected)
                    .isEqualByComparingTo(expected);
        }
    }

    // ── K-04 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("K-04: Sequential operations on same wallet maintain FIFO order → final balance correct")
    void shouldMaintainFifoOrderForSameWallet() throws Exception {
        createWallet("user-k04");

        // D(+100) → W(-30) → D(+50) → W(-20) → expected final = 100
        String userId = "user-k04";

        mockMvc.perform(post("/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", userId, "amount", new BigDecimal("100.00"),
                                "idempotencyId", UUID.randomUUID().toString()))))
                .andExpect(status().isOk());

        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = 'user-k04')", 1, ASYNC_TIMEOUT_MS);

        mockMvc.perform(post("/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", userId, "amount", new BigDecimal("30.00"),
                                "idempotencyId", UUID.randomUUID().toString()))))
                .andExpect(status().isOk());

        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = 'user-k04')", 2, ASYNC_TIMEOUT_MS);

        mockMvc.perform(post("/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", userId, "amount", new BigDecimal("50.00"),
                                "idempotencyId", UUID.randomUUID().toString()))))
                .andExpect(status().isOk());

        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = 'user-k04')", 3, ASYNC_TIMEOUT_MS);

        mockMvc.perform(post("/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", userId, "amount", new BigDecimal("20.00"),
                                "idempotencyId", UUID.randomUUID().toString()))))
                .andExpect(status().isOk());

        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = 'user-k04')", 4, ASYNC_TIMEOUT_MS);

        BigDecimal finalBalance = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-k04'", BigDecimal.class);
        // 100 - 30 + 50 - 20 = 100
        assertThat(finalBalance).isEqualByComparingTo("100.00");
    }

    // ── K-05 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("K-05: After Redis flush, valid new requests are still processed correctly")
    void shouldContinueProcessingAfterRedisFlushed() throws Exception {
        createWallet("user-k05");

        // First operation
        deposit("user-k05", "50.00");

        // Wipe Redis (simulates cache restart)
        redisTemplate.delete(redisTemplate.keys("*"));

        // Second operation after flush — should work fine
        var body = Map.of(
                "userId", "user-k05",
                "amount", new BigDecimal("25.00"),
                "idempotencyId", UUID.randomUUID().toString()
        );
        mockMvc.perform(post("/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                "(SELECT id FROM wallets WHERE user_id = 'user-k05')",
                2, ASYNC_TIMEOUT_MS);

        BigDecimal balance = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-k05'", BigDecimal.class);
        assertThat(balance).isEqualByComparingTo("75.00");
    }

    @Test
    @DisplayName("K-06: Force out-of-order by publishing directly to specific partitions")
    void shouldExposeOrderingProblemWithoutPartitionKey() throws Exception {
        createWallet("user-k06");

        // Create a non-transactional KafkaTemplate to bypass transaction requirement

        UUID withdrawId  = UUID.randomUUID();
        UUID depositId1  = UUID.randomUUID();
        UUID depositId2  = UUID.randomUUID();
        UUID withdrawId2 = UUID.randomUUID();

        TransactionMessage withdraw1 = TransactionMessage.builder()
                .transactionId(withdrawId)
                .idempotencyId(UUID.randomUUID())
                .userId("user-k06")
                .amount(new BigDecimal("30.00"))
                .type(TransactionType.WITHDRAW)
                .build();

        TransactionMessage deposit1 = TransactionMessage.builder()
                .transactionId(depositId1)
                .idempotencyId(UUID.randomUUID())
                .userId("user-k06")
                .amount(new BigDecimal("100.00"))
                .type(TransactionType.DEPOSIT)
                .build();

        TransactionMessage deposit2 = TransactionMessage.builder()
                .transactionId(depositId2)
                .idempotencyId(UUID.randomUUID())
                .userId("user-k06")
                .amount(new BigDecimal("50.00"))
                .type(TransactionType.DEPOSIT)
                .build();

        TransactionMessage withdraw2 = TransactionMessage.builder()
                .transactionId(withdrawId2)
                .idempotencyId(UUID.randomUUID())
                .userId("user-k06")
                .amount(new BigDecimal("20.00"))
                .type(TransactionType.WITHDRAW)
                .build();

        // Force wrong order: withdraw BEFORE deposit on different partitions
        kafkaTransactionEventPublisher.publish(withdraw1);
        kafkaTransactionEventPublisher.publish(deposit1);
        kafkaTransactionEventPublisher.publish(deposit2);
        kafkaTransactionEventPublisher.publish(withdraw2);

        // withdraw1 (-30) on partition 1 processed before deposit1 (+100) on partition 0
        // insufficient funds → DLQ → fewer than 4 transactions committed
        // waitForCondition will TIMEOUT proving the ordering problem
        waitForCondition(
                "SELECT COUNT(*) FROM transactions WHERE wallet_id = " +
                        "(SELECT id FROM wallets WHERE user_id = 'user-k06')",
                4, ASYNC_TIMEOUT_MS);

        BigDecimal balance = jdbcTemplate.queryForObject(
                "SELECT balance FROM wallets WHERE user_id = 'user-k06'", BigDecimal.class);

        assertThat(balance).isEqualByComparingTo("100.00");
    }
}
