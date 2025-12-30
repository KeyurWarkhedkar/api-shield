package com.keyur.api_shield.Services;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Service
public class TokenBucketService {

    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<List> tokenBucketScript;

    public TokenBucketService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;

        //load Lua script
        tokenBucketScript = new DefaultRedisScript<>();
        tokenBucketScript.setScriptText(loadLuaScript("token-bucket.lua"));
        tokenBucketScript.setResultType(List.class);
    }

    /**
     * Try to consume tokens from the bucket.
     *
     * @param keyPrefix      Unique key prefix (e.g., "user:123")
     * @param tokensToConsume Number of tokens to consume
     * @param capacity       Max bucket capacity
     * @param refillRate     Tokens per second refill rate
     * @return true if request is allowed, false if throttled
     */
    public boolean tryConsume(String keyPrefix, int tokensToConsume, int capacity, int refillRate) {
        String tokenKey = keyPrefix + ":tokens";
        String refillKey = keyPrefix + ":lastRefill";

        //System.out.println(tokenKey);
        //System.out.println(refillKey);

        //lazy initialization: create keys if they don't exist


        //execute Lua script
        List<Long> result = (List<Long>) stringRedisTemplate.execute(
                tokenBucketScript,
                Arrays.asList(tokenKey, refillKey),
                String.valueOf(capacity),
                String.valueOf(refillRate),
                String.valueOf(System.currentTimeMillis())
        );

        //System.out.println(result);

        return result != null && result.get(0) == 1;
    }

    /**
     * Load Lua script from resources/redis
     */
    private String loadLuaScript(String fileName) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("redis/" + fileName)) {
            if (is == null) throw new RuntimeException("Lua script not found: " + fileName);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
