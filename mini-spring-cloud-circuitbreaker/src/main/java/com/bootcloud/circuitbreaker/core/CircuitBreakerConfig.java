package com.bootcloud.circuitbreaker.core;

public class CircuitBreakerConfig {
    private String name;
    private int failureThreshold;
    private long timeout;
    private long resetTimeout;
    private boolean enableMetrics;

    public CircuitBreakerConfig() {
        this.failureThreshold = 5;
        this.timeout = 60000;
        this.resetTimeout = 60000;
        this.enableMetrics = true;
    }

    public CircuitBreakerConfig(String name) {
        this();
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getFailureThreshold() {
        return failureThreshold;
    }

    public void setFailureThreshold(int failureThreshold) {
        this.failureThreshold = failureThreshold;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public long getResetTimeout() {
        return resetTimeout;
    }

    public void setResetTimeout(long resetTimeout) {
        this.resetTimeout = resetTimeout;
    }

    public boolean isEnableMetrics() {
        return enableMetrics;
    }

    public void setEnableMetrics(boolean enableMetrics) {
        this.enableMetrics = enableMetrics;
    }

    public static CircuitBreakerConfig.Builder builder() {
        return new CircuitBreakerConfig.Builder();
    }

    public static class Builder {
        private final CircuitBreakerConfig config = new CircuitBreakerConfig();

        public Builder name(String name) {
            config.name = name;
            return this;
        }

        public Builder failureThreshold(int failureThreshold) {
            config.failureThreshold = failureThreshold;
            return this;
        }

        public Builder timeout(long timeout) {
            config.timeout = timeout;
            return this;
        }

        public Builder resetTimeout(long resetTimeout) {
            config.resetTimeout = resetTimeout;
            return this;
        }

        public Builder enableMetrics(boolean enableMetrics) {
            config.enableMetrics = enableMetrics;
            return this;
        }

        public CircuitBreakerConfig build() {
            return config;
        }
    }
}
