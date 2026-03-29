package com.bootcloud.circuitbreaker.factory;

import com.bootcloud.circuitbreaker.core.CircuitBreaker;
import com.bootcloud.circuitbreaker.core.CircuitBreakerConfig;
import com.bootcloud.circuitbreaker.core.impl.DefaultCircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CircuitBreakerFactory {
    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerFactory.class);
    
    private static final Map<String, CircuitBreaker> circuitBreakerCache = new ConcurrentHashMap<>();

    public static CircuitBreaker create(CircuitBreakerConfig config) {
        return circuitBreakerCache.computeIfAbsent(config.getName(), name -> {
            log.debug("Creating new CircuitBreaker with config: {}", config);
            return new DefaultCircuitBreaker(config);
        });
    }

    public static CircuitBreaker create(String name) {
        return create(CircuitBreakerConfig.builder().name(name).build());
    }

    public static CircuitBreaker create(String name, int failureThreshold, long resetTimeout) {
        return create(CircuitBreakerConfig.builder()
                .name(name)
                .failureThreshold(failureThreshold)
                .resetTimeout(resetTimeout)
                .build());
    }

    public static CircuitBreaker get(String name) {
        return circuitBreakerCache.get(name);
    }

    public static void remove(String name) {
        CircuitBreaker removed = circuitBreakerCache.remove(name);
        if (removed != null) {
            log.debug("Removed CircuitBreaker: {}", name);
        }
    }

    public static void clearAll() {
        log.info("Clearing all CircuitBreakers");
        circuitBreakerCache.clear();
    }
}
