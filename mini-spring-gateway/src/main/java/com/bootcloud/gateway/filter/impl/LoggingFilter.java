package com.bootcloud.gateway.filter.impl;

import com.bootcloud.gateway.filter.GatewayFilter;
import com.bootcloud.gateway.filter.GatewayFilterChain;
import com.bootcloud.gateway.model.GatewayRequest;
import com.bootcloud.gateway.model.GatewayResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingFilter implements GatewayFilter {
    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public GatewayResponse filter(GatewayRequest request, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        log.info("Incoming request: {} {}", request.getMethod(), request.getPath());
        
        GatewayResponse response = chain.doFilter(request);
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("Request completed: {} {} - Status: {} - Duration: {}ms", 
                request.getMethod(), request.getPath(), response.getStatus(), duration);
        
        return response;
    }
}
