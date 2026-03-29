package com.bootcloud.circuitbreaker.core.impl;

import com.bootcloud.circuitbreaker.core.CircuitBreaker;
import com.bootcloud.circuitbreaker.core.CircuitBreakerConfig;
import com.bootcloud.circuitbreaker.core.CircuitBreakerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class DefaultCircuitBreaker implements CircuitBreaker {
    private static final Logger log = LoggerFactory.getLogger(DefaultCircuitBreaker.class);
    
    private final CircuitBreakerConfig config;
    private final AtomicReference<CircuitBreakerState> state;
    private final AtomicInteger failureCount;
    private final AtomicInteger successCount;
    private final AtomicReference<Instant> lastFailureTime;
    private final AtomicReference<Instant> openTime;
    private final AtomicInteger totalRequests;
    private final AtomicInteger totalFailures;

    public DefaultCircuitBreaker(CircuitBreakerConfig config) {
        this.config = config;
        this.state = new AtomicReference<>(CircuitBreakerState.CLOSED);
        this.failureCount = new AtomicInteger(0);
        this.successCount = new AtomicInteger(0);
        this.lastFailureTime = new AtomicReference<>(Instant.now());
        this.openTime = new AtomicReference<>(null);
        this.totalRequests = new AtomicInteger(0);
        this.totalFailures = new AtomicInteger(0);
    }

    @Override
    public <T> T execute(Callable<T> callable) throws Exception {
        return execute(callable, null);
    }

    @Override
    public <T> T execute(Callable<T> callable, Callable<T> fallback) throws Exception {
        totalRequests.incrementAndGet();
        
        if (!allowRequest()) {
            log.warn("Circuit breaker is OPEN, rejecting request for: {}", config.getName());
            totalFailures.incrementAndGet();
            if (fallback != null) {
                log.info("Executing fallback method for: {}", config.getName());
                return fallback.call();
            }
            throw new CircuitBreakerOpenException("Circuit breaker is OPEN for: " + config.getName());
        }

        try {
            T result = callable.call();
            recordSuccess();
            return result;
        } catch (Exception e) {
            recordFailure();
            if (fallback != null) {
                log.info("Executing fallback method due to exception for: {}", config.getName());
                return fallback.call();
            }
            throw e;
        }
    }

    @Override
    public void recordSuccess() {
        successCount.incrementAndGet();
        failureCount.set(0);
        
        if (state.get() == CircuitBreakerState.HALF_OPEN) {
            log.info("Circuit breaker transitioning to CLOSED for: {}", config.getName());
            state.set(CircuitBreakerState.CLOSED);
        }
        
        if (config.isEnableMetrics()) {
            logMetrics();
        }
    }

    @Override
    public void recordFailure() {
        failureCount.incrementAndGet();
        totalFailures.incrementAndGet();
        lastFailureTime.set(Instant.now());
        
        if (failureCount.get() >= config.getFailureThreshold()) {
            log.warn("Circuit breaker transitioning to OPEN for: {}", config.getName());
            state.set(CircuitBreakerState.OPEN);
            openTime.set(Instant.now());
        }
        
        if (config.isEnableMetrics()) {
            logMetrics();
        }
    }

    @Override
    public CircuitBreakerState getState() {
        return state.get();
    }

    @Override
    public void reset() {
        log.info("Resetting circuit breaker for: {}", config.getName());
        state.set(CircuitBreakerState.CLOSED);
        failureCount.set(0);
        successCount.set(0);
        openTime.set(null);
        totalRequests.set(0);
        totalFailures.set(0);
    }

    @Override
    public CircuitBreakerConfig getConfig() {
        return config;
    }

    private boolean allowRequest() {
        CircuitBreakerState currentState = state.get();
        
        switch (currentState) {
            case CLOSED:
                return true;
                
            case OPEN:
                if (shouldAttemptReset()) {
                    log.info("Circuit breaker transitioning to HALF_OPEN for: {}", config.getName());
                    state.set(CircuitBreakerState.HALF_OPEN);
                    return true;
                }
                return false;
                
            case HALF_OPEN:
                return true;
                
            default:
                return false;
        }
    }

    private boolean shouldAttemptReset() {
        Instant openInstant = openTime.get();
        if (openInstant == null) {
            return false;
        }
        
        Duration timeSinceOpen = Duration.between(openInstant, Instant.now());
        return timeSinceOpen.toMillis() >= config.getResetTimeout();
    }

    private void logMetrics() {
        log.debug("Circuit Breaker [{}] - State: {}, Failures: {}, Successes: {}, Total Requests: {}, Total Failures: {}", 
                config.getName(), 
                state.get(), 
                failureCount.get(), 
                successCount.get(),
                totalRequests.get(),
                totalFailures.get());
    }

    public CircuitBreakerMetrics getMetrics() {
        return new CircuitBreakerMetrics(
                state.get(),
                failureCount.get(),
                successCount.get(),
                totalRequests.get(),
                totalFailures.get(),
                lastFailureTime.get(),
                openTime.get()
        );
    }

    public static class CircuitBreakerMetrics {
        private final CircuitBreakerState state;
        private final int failureCount;
        private final int successCount;
        private final int totalRequests;
        private final int totalFailures;
        private final Instant lastFailureTime;
        private final Instant openTime;

        public CircuitBreakerMetrics(CircuitBreakerState state, int failureCount, int successCount, 
                                     int totalRequests, int totalFailures, 
                                     Instant lastFailureTime, Instant openTime) {
            this.state = state;
            this.failureCount = failureCount;
            this.successCount = successCount;
            this.totalRequests = totalRequests;
            this.totalFailures = totalFailures;
            this.lastFailureTime = lastFailureTime;
            this.openTime = openTime;
        }

        public CircuitBreakerState getState() {
            return state;
        }

        public int getFailureCount() {
            return failureCount;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getTotalRequests() {
            return totalRequests;
        }

        public int getTotalFailures() {
            return totalFailures;
        }

        public Instant getLastFailureTime() {
            return lastFailureTime;
        }

        public Instant getOpenTime() {
            return openTime;
        }

        public double getFailureRate() {
            return totalRequests == 0 ? 0 : (double) totalFailures / totalRequests;
        }

        @Override
        public String toString() {
            return "CircuitBreakerMetrics{" +
                    "state=" + state +
                    ", failureCount=" + failureCount +
                    ", successCount=" + successCount +
                    ", totalRequests=" + totalRequests +
                    ", totalFailures=" + totalFailures +
                    ", failureRate=" + String.format("%.2f%%", getFailureRate() * 100) +
                    ", lastFailureTime=" + lastFailureTime +
                    ", openTime=" + openTime +
                    '}';
        }
    }
}
