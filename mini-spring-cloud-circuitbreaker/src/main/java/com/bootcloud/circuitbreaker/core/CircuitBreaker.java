package com.bootcloud.circuitbreaker.core;

import java.util.concurrent.Callable;

public interface CircuitBreaker {
    <T> T execute(Callable<T> callable) throws Exception;

    <T> T execute(Callable<T> callable, Callable<T> fallback) throws Exception;

    void recordSuccess();

    void recordFailure();

    CircuitBreakerState getState();

    void reset();

    CircuitBreakerConfig getConfig();
}
