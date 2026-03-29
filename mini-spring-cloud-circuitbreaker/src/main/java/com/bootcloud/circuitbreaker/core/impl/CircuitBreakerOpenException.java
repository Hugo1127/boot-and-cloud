package com.bootcloud.circuitbreaker.core.impl;

public class CircuitBreakerOpenException extends RuntimeException {
    public CircuitBreakerOpenException(String message) {
        super(message);
    }

    public CircuitBreakerOpenException(String message, Throwable cause) {
        super(message, cause);
    }
}
