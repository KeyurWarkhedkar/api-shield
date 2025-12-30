package com.keyur.api_shield.Services;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisTrafficCounter {

    //rolling window for counting requests
    private static final Duration WINDOW = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;

    public RedisTrafficCounter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void increment(String method, String normalizedPath) {
        String key = "api:freq:" + method + ":" + normalizedPath;

        Long count = redisTemplate.opsForValue().increment(key);

        //if first hit, set TTL
        if (count != null && count == 1) {
            redisTemplate.expire(key, WINDOW);
        }
    }

    public Long getCount(String method, String normalizedPath) {
        String key = buildKey(method, normalizedPath);
        String value = redisTemplate.opsForValue().get(key);
        return value == null ? 0L : Long.parseLong(value);
    }

    private String buildKey(String method, String normalizedPath) {
        return "api:freq:" + method + ":" + normalizedPath;
    }
}
