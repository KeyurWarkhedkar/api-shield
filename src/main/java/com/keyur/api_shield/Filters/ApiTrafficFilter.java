package com.keyur.api_shield.Filters;

import com.keyur.api_shield.Services.PathNormalizer;
import com.keyur.api_shield.Services.RedisTrafficCounter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10) // Run first
public class ApiTrafficFilter extends OncePerRequestFilter {

    private final PathNormalizer pathNormalizer;
    private final RedisTrafficCounter trafficCounter;

    public ApiTrafficFilter(PathNormalizer pathNormalizer,
                            RedisTrafficCounter trafficCounter) {
        this.pathNormalizer = pathNormalizer;
        this.trafficCounter = trafficCounter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod();

        if ("GET".equalsIgnoreCase(method)) {
            // Normalize path for traffic aggregation (wildcard IDs)
            String normalizedPathForTraffic = pathNormalizer.normalizeForTraffic(request.getRequestURI());
            trafficCounter.increment(method, normalizedPathForTraffic);
        }

        // Continue filter chain
        filterChain.doFilter(request, response);
    }
}

