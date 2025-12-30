package com.keyur.api_shield.Filters;

import com.keyur.api_shield.Services.PathNormalizer;
import com.keyur.api_shield.Services.RedisTrafficCounter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Collectors;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20) // Run after traffic filter
public class DynamicCacheFilter extends OncePerRequestFilter {

    private final RedisTrafficCounter trafficCounter;
    private final StringRedisTemplate redisTemplate;
    private final PathNormalizer pathNormalizer;

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final long HOT_THRESHOLD = 50L;

    public DynamicCacheFilter(RedisTrafficCounter trafficCounter,
                              StringRedisTemplate redisTemplate,
                              PathNormalizer pathNormalizer) {
        this.trafficCounter = trafficCounter;
        this.redisTemplate = redisTemplate;
        this.pathNormalizer = pathNormalizer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod();
        if (!"GET".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check if endpoint is hot
        String normalizedPathForTraffic = pathNormalizer.normalizeForTraffic(request.getRequestURI());
        System.out.println(normalizedPathForTraffic); // could use pathNormalizer if needed
        Long hits = trafficCounter.getCount(method, normalizedPathForTraffic);

        if (hits != null && hits >= HOT_THRESHOLD) {
            // Exact path for caching (do not normalize)
            String cacheKey = buildCacheKey(method, request.getRequestURI(), request.getQueryString());

            // Try Redis cache
            String cachedResponse = redisTemplate.opsForValue().get(cacheKey);
            System.out.println("Cache hit!");
            if (cachedResponse != null) {
                response.setContentType("application/json");
                response.getWriter().write(cachedResponse);
                return;
            }

            // Wrap response to capture body
            ContentCachingResponseWrapper cachingResponse = new ContentCachingResponseWrapper(response);
            filterChain.doFilter(request, cachingResponse);

            String body = new String(cachingResponse.getContentAsByteArray(), StandardCharsets.UTF_8);
            redisTemplate.opsForValue().set(cacheKey, body, CACHE_TTL);

            cachingResponse.copyBodyToResponse();
            return;
        }

        // Non-hot endpoints
        filterChain.doFilter(request, response);
    }

    private String buildCacheKey(String method, String path, String query) {
        if (query == null || query.isBlank()) {
            return "cache:" + method + ":" + path;
        } else {
            return "cache:" + method + ":" + path + "?" + normalizeQuery(query);
        }
    }

    private String normalizeQuery(String queryString) {
        if (queryString == null || queryString.isBlank()) return "";
        return Arrays.stream(queryString.split("&"))
                .map(String::trim)
                .sorted()
                .collect(Collectors.joining("&"));
    }
}
