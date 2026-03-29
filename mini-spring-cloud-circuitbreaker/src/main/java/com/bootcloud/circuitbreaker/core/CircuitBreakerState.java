package com.bootcloud.circuitbreaker.core;

public enum CircuitBreakerState {
    CLOSED,
    OPEN,
    HALF_OPEN
}
