package br.com.wallet.project.infrastructure.cache.redis;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RedisOperation {
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${wallet.redis.timeout}")
    private Long timeout;

    @Value("${wallet.redis.unit}")
    private TimeUnit unit;

    private ValueOperations<String, String> operations;

    public RedisOperation(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void afterPropertiesSet() {
        operations = redisTemplate.opsForValue();
    }


    public boolean saveIfNotExists(String key, String value) {
        Boolean isNew = operations.setIfAbsent(key, value, timeout, unit);
        return Boolean.TRUE.equals(isNew);
    }

    public void save(String key, String value) {
        operations.set(key, value, timeout, unit);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }
}