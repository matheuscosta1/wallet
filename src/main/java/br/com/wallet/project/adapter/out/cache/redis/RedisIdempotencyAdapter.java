package br.com.wallet.project.adapter.out.cache.redis;
import br.com.wallet.project.application.port.out.IdempotencyRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;
/** Driven adapter: implements IdempotencyRepository port using Redis. */
@Slf4j
@Component
public class RedisIdempotencyAdapter implements IdempotencyRepository {
    private final RedisTemplate<String, String> redisTemplate;
    @Value("${wallet.redis.timeout}") private Long timeout;
    @Value("${wallet.redis.unit}") private TimeUnit unit;
    private ValueOperations<String, String> ops;
    public RedisIdempotencyAdapter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    @PostConstruct
    public void init() { ops = redisTemplate.opsForValue(); }
    @Override
    public boolean registerIfAbsent(String key) {
        return Boolean.TRUE.equals(ops.setIfAbsent(key, "IN_PROGRESS", timeout, unit));
    }
    @Override
    public void markAsCompleted(String key) { ops.set(key, "COMPLETED", timeout, unit); }

    @Override
    public void release(String key) { redisTemplate.delete(key); }
}
