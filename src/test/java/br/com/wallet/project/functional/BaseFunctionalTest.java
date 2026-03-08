package br.com.wallet.project.functional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Base class for all functional (integration) tests.
 * Spins up PostgreSQL, Kafka and Redis via Testcontainers.
 * All test containers are shared across the test suite (static) for performance.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public abstract class BaseFunctionalTest implements TestContainerSetup {
    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @Autowired
    protected RedisTemplate<String, String> redisTemplate;
    @Autowired
    private KafkaTemplate<?, ?> kafkaTemplate;
    // ── Clean state before each test ──────────────────────────────────────────

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM transfers");
        jdbcTemplate.execute("DELETE FROM transactions");
        jdbcTemplate.execute("DELETE FROM wallets");
    }

    @BeforeEach
    void cleanRedis() {
        Set<String> keys = redisTemplate.keys("*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * Resets the Kafka transactional producer between tests.
     *
     * <p>Why this is needed: Kafka's transactional producer is stateful — the broker
     * tracks a (transactional-id → PID + epoch) mapping. When a test ends and the next
     * one starts, the broker may have already invalidated the previous PID (transaction
     * timeout, broker restart, etc.). If the Spring {@link ProducerFactory} is not reset,
     * it hands out a cached producer whose PID is now stale, and the broker rejects it
     * with {@code InvalidPidMappingException}.
     *
     * <p>Calling {@link ProducerFactory#reset()} destroys all cached producers in the
     * pool. The next {@code send()} call will create a fresh producer and let the broker
     * assign a new PID, which solves the problem cleanly without requiring any changes
     * to the application code or Kafka configuration.
     */
    @BeforeEach
    void resetKafkaProducer() {
        ProducerFactory<?, ?> producerFactory = kafkaTemplate.getProducerFactory();
        producerFactory.reset();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Waits up to {@code timeoutMs} for a row count condition to be met.
     * Useful when asserting on async Kafka-processed results.
     */
    protected void waitForCondition(String sql, int expectedCount, long timeoutMs)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            if (count != null && count == expectedCount) return;
            Thread.sleep(200);
        }
        throw new AssertionError("Condition not met within " + timeoutMs + "ms. SQL: " + sql);
    }

    /**
     * Convenience overload: waits for the balance to reach exactly {@code expectedBalance}
     * (using {@code compareTo == 0}).
     */
    protected void waitForExactBalance(String userId, BigDecimal expectedBalance, long timeoutMs)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try {
                BigDecimal actual = jdbcTemplate.queryForObject(
                        "SELECT balance FROM wallets WHERE user_id = ?", BigDecimal.class, userId);
                if (actual != null && actual.compareTo(expectedBalance) == 0) return;
            } catch (Exception ignored) { }
            Thread.sleep(200);
        }
        BigDecimal actual = null;
        try {
            actual = jdbcTemplate.queryForObject(
                    "SELECT balance FROM wallets WHERE user_id = ?", BigDecimal.class, userId);
        } catch (Exception ignored) { }
        throw new AssertionError(
                "waitForExactBalance timed out after " + timeoutMs + "ms for user='" + userId + "'. " +
                        "Expected balance=" + expectedBalance + " but got=" + actual);
    }
}
