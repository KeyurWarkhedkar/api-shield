package com.keyur.api_shield.Controllers;

import com.keyur.api_shield.Services.TokenBucketService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/proxy")
public class RateLimiterProxyController {

    private final TokenBucketService tokenBucketService;
    private final RestTemplate restTemplate;

    @Value("${proxy.external.base-url}")
    private String externalBaseUrl;

    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger forwardedRequests = new AtomicInteger(0);
    private final AtomicInteger throttledRequests = new AtomicInteger(0);
    private final Map<String, AtomicInteger> forwardedPerIP = new ConcurrentHashMap<>();

    public RateLimiterProxyController(TokenBucketService tokenBucketService) {
        this.tokenBucketService = tokenBucketService;
        this.restTemplate = new RestTemplate();
    }

    @RequestMapping("/**")
    public ResponseEntity<?> proxyAll(HttpServletRequest request,
                                      @RequestBody(required = false) String body) {

        totalRequests.incrementAndGet();

        // Get client IP
        String clientIP = request.getHeader("X-Forwarded-For");
        if (clientIP == null) clientIP = request.getRemoteAddr();
        String keyPrefix = "user:" + clientIP;

        // Rate limit check
        boolean allowed = tokenBucketService.tryConsume(keyPrefix, 1, 5, 1); // capacity/refill
        if (!allowed) {
            throttledRequests.incrementAndGet();
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Rate limit exceeded");
        }

        // Update forwarded metrics
        forwardedRequests.incrementAndGet();
        forwardedPerIP.computeIfAbsent(clientIP, k -> new AtomicInteger(0))
                .incrementAndGet();

        // Build external URL
        String externalUrl = externalBaseUrl + request.getRequestURI().replace("/proxy", "");
        if (request.getQueryString() != null) {
            externalUrl += "?" + request.getQueryString();
        }

        // Forward headers
        HttpHeaders headers = new HttpHeaders();
        Collections.list(request.getHeaderNames())
                .forEach(name -> headers.add(name, request.getHeader(name)));

        // Determine HTTP method
        HttpMethod method;
        try {
            method = HttpMethod.valueOf(request.getMethod().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                    .body("HTTP method not supported: " + request.getMethod());
        }

        // Forward request to external API
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> externalResponse;
        try {
            System.out.println(externalUrl);
            externalResponse = restTemplate.exchange(externalUrl, method, entity, String.class);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Failed to contact external service: " + e.getMessage());
        }

        // Return external API response as-is
        return ResponseEntity.status(externalResponse.getStatusCode())
                .headers(externalResponse.getHeaders())
                .body(externalResponse.getBody());
    }

    @GetMapping("/metrics")
    public Map<String, Object> metrics() {
        return Map.of(
                "totalRequests", totalRequests.get(),
                "forwardedRequests", forwardedRequests.get(),
                "throttledRequests", throttledRequests.get(),
                "forwardedPerIP", forwardedPerIP
        );
    }
}
