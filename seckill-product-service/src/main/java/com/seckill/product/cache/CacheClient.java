package com.seckill.product.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Component
public class CacheClient {

    private static final String EMPTY_MARKER = "null";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${cache.product.ttl-seconds:1800}")
    private long productTtlSeconds;

    @Value("${cache.product.null-ttl-seconds:120}")
    private long nullTtlSeconds;

    @Value("${cache.product.lock-ttl-seconds:10}")
    private long lockTtlSeconds;

    public CacheClient(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public <T, ID> T queryWithPassThroughAndMutex(
            String keyPrefix,
            String lockPrefix,
            ID id,
            Class<T> type,
            Function<ID, T> dbFallback) {
        String key = keyPrefix + id;
        String cacheValue = stringRedisTemplate.opsForValue().get(key);
        if (cacheValue != null) {
            if (EMPTY_MARKER.equals(cacheValue)) {
                return null;
            }
            return fromJson(cacheValue, type);
        }

        String lockKey = lockPrefix + id;
        boolean lockAcquired = tryLock(lockKey);
        if (!lockAcquired) {
            sleepMillis(50);
            String retryValue = stringRedisTemplate.opsForValue().get(key);
            if (retryValue != null) {
                if (EMPTY_MARKER.equals(retryValue)) {
                    return null;
                }
                return fromJson(retryValue, type);
            }
            return dbFallback.apply(id);
        }

        try {
            T dbData = dbFallback.apply(id);
            if (dbData == null) {
                stringRedisTemplate.opsForValue().set(key, EMPTY_MARKER, Duration.ofSeconds(nullTtlSeconds));
                return null;
            }
            long ttlWithJitter = productTtlSeconds + ThreadLocalRandom.current().nextLong(60, 301);
            stringRedisTemplate.opsForValue().set(key, toJson(dbData), Duration.ofSeconds(ttlWithJitter));
            return dbData;
        } finally {
            unlock(lockKey);
        }
    }

    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    private boolean tryLock(String key) {
        Boolean result = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", lockTtlSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

    private void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("缓存序列化失败", e);
        }
    }

    private <T> T fromJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("缓存反序列化失败", e);
        }
    }
}
