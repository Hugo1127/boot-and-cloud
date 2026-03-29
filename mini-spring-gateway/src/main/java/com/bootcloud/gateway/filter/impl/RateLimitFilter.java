package com.bootcloud.gateway.filter.impl;

import com.bootcloud.gateway.filter.GatewayFilter;
import com.bootcloud.gateway.filter.GatewayFilterChain;
import com.bootcloud.gateway.model.GatewayRequest;
import com.bootcloud.gateway.model.GatewayResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimitFilter implements GatewayFilter {
    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    
    private final int maxRequestsPerMinute;
    private final ConcurrentMap<String, AtomicInteger> requestCountMap;
    private final ConcurrentMap<String, Long> lastResetTimeMap;

    public RateLimitFilter(int maxRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
        this.requestCountMap = new ConcurrentHashMap<>();
        this.lastResetTimeMap = new ConcurrentHashMap<>();
    }

    @Override
    public GatewayResponse filter(GatewayRequest request, GatewayFilterChain chain) {
        String clientId = getClientId(request);
        
        if (!checkRateLimit(clientId)) {
            log.warn("Rate limit exceeded for client: {}", clientId);
            return GatewayResponse.tooManyRequests("Rate limit exceeded");
        }
        
        return chain.doFilter(request);
    }

    private boolean checkRateLimit(String clientId) {
        long currentTime = System.currentTimeMillis();
        Long lastResetTime = lastResetTimeMap.get(clientId);
        
        if (lastResetTime == null || currentTime - lastResetTime > 60000) {
            lastResetTimeMap.put(clientId, currentTime);
            requestCountMap.put(clientId, new AtomicInteger(1));
            return true;
        }
        
        AtomicInteger count = requestCountMap.get(clientId);
        if (count.incrementAndGet() > maxRequestsPerMinute) {
            return false;
        }
        
        return true;
    }

    private String getClientId(GatewayRequest request) {
        String clientId = request.getHeaders().get("X-Client-Id");
        if (clientId == null || clientId.isEmpty()) {
            clientId = request.getHeaders().get("X-Forwarded-For");
        }
        if (clientId == null || clientId.isEmpty()) {
            clientId = "default";
        }
        return clientId;
    }
}
