package br.com.wallet.project.functional;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Set;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public abstract class BaseFunctionalTest {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected JdbcTemplate jdbcTemplate;
    @Autowired protected RedisTemplate<String, String> redisTemplate;
    @Autowired private KafkaTemplate<?, ?> kafkaTemplate;

    /**
     * Ponto único de configuração dinâmica de properties.
     * Delega ao TestPropertyOverride, que por sua vez consulta o ContainerManager.
     * O ContainerManager decide em tempo de inicialização se usa Docker local
     * ou sobe containers via Testcontainers.
     */
    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        TestPropertyOverride.register(registry);
    }

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM transfers");
        jdbcTemplate.execute("DELETE FROM transactions");
        jdbcTemplate.execute("DELETE FROM wallets");
        jdbcTemplate.execute("DELETE FROM wallet_events");
    }

    @BeforeEach
    void cleanRedis() {
        Set<String> keys = redisTemplate.keys("*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @BeforeEach
    void resetKafkaProducer() {
        ProducerFactory<?, ?> pf = kafkaTemplate.getProducerFactory();
        pf.reset();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    protected void waitForCondition(String sql, int expectedCount, long timeoutMs)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            if (count != null && count == expectedCount) return;
            Thread.sleep(200);
        }
        throw new AssertionError(
                "Condition not met within " + timeoutMs + "ms. SQL: " + sql);
    }

    protected void waitForExactBalance(String userId, BigDecimal expected, long timeoutMs)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try {
                BigDecimal actual = jdbcTemplate.queryForObject(
                        "SELECT balance FROM wallets WHERE user_id = ?",
                        BigDecimal.class, userId);
                if (actual != null && actual.compareTo(expected) == 0) return;
            } catch (Exception ignored) {}
            Thread.sleep(200);
        }
        BigDecimal actual = null;
        try {
            actual = jdbcTemplate.queryForObject(
                    "SELECT balance FROM wallets WHERE user_id = ?",
                    BigDecimal.class, userId);
        } catch (Exception ignored) {}
        throw new AssertionError(
                "waitForExactBalance timed out after " + timeoutMs + "ms for user='"
                        + userId + "'. Expected=" + expected + " but got=" + actual);
    }
}