package com.bootcloud.circuitbreaker.test;

import com.bootcloud.circuitbreaker.core.CircuitBreaker;
import com.bootcloud.circuitbreaker.core.CircuitBreakerConfig;
import com.bootcloud.circuitbreaker.core.CircuitBreakerState;
import com.bootcloud.circuitbreaker.core.impl.CircuitBreakerOpenException;
import com.bootcloud.circuitbreaker.core.impl.DefaultCircuitBreaker;
import com.bootcloud.circuitbreaker.factory.CircuitBreakerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;

public class CircuitBreakerTest {
    private CircuitBreaker circuitBreaker;

    @BeforeEach
    public void setUp() {
        CircuitBreakerConfig config = CircuitBreakerConfig.builder()
                .name("test-circuit-breaker")
                .failureThreshold(3)
                .timeout(1000)
                .resetTimeout(1000)
                .build();
        circuitBreaker = new DefaultCircuitBreaker(config);
    }

    @AfterEach
    public void tearDown() {
        CircuitBreakerFactory.clearAll();
    }

    @Test
    public void testInitialState() {
        assertEquals(CircuitBreakerState.CLOSED, circuitBreaker.getState());
    }

    @Test
    public void testSuccessExecution() throws Exception {
        Callable<String> callable = () -> "success";
        
        String result = circuitBreaker.execute(callable);
        
        assertEquals("success", result);
        assertEquals(CircuitBreakerState.CLOSED, circuitBreaker.getState());
    }

    @Test
    public void testFailureExecution() throws Exception {
        Callable<String> callable = () -> {
            throw new RuntimeException("Test exception");
        };
        
        assertThrows(RuntimeException.class, () -> circuitBreaker.execute(callable));
        assertEquals(CircuitBreakerState.CLOSED, circuitBreaker.getState());
    }

    @Test
    public void testCircuitBreakerOpens() throws Exception {
        Callable<String> callable = () -> {
            throw new RuntimeException("Test exception");
        };
        
        for (int i = 0; i < 3; i++) {
            assertThrows(RuntimeException.class, () -> circuitBreaker.execute(callable));
        }
        
        assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState());
    }

    @Test
    public void testCircuitBreakerBlocksRequestsWhenOpen() throws Exception {
        Callable<String> callable = () -> {
            throw new RuntimeException("Test exception");
        };
        
        for (int i = 0; i < 3; i++) {
            assertThrows(RuntimeException.class, () -> circuitBreaker.execute(callable));
        }
        
        assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState());
        
        assertThrows(CircuitBreakerOpenException.class, () -> circuitBreaker.execute(callable));
    }

    @Test
    public void testFallbackMethod() throws Exception {
        Callable<String> callable = () -> {
            throw new RuntimeException("Test exception");
        };
        
        Callable<String> fallback = () -> "fallback";
        
        for (int i = 0; i < 3; i++) {
            String result = circuitBreaker.execute(callable, fallback);
            assertEquals("fallback", result);
        }
        
        String result = circuitBreaker.execute(callable, fallback);
        assertEquals("fallback", result);
    }

    @Test
    public void testCircuitBreakerResetsAfterTimeout() throws Exception {
        Callable<String> callable = () -> {
            throw new RuntimeException("Test exception");
        };
        
        for (int i = 0; i < 3; i++) {
            assertThrows(RuntimeException.class, () -> circuitBreaker.execute(callable));
        }
        
        assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState());
        
        Thread.sleep(1500);
        
        Callable<String> successCallable = () -> "success";
        String result = circuitBreaker.execute(successCallable);
        
        assertEquals("success", result);
        assertEquals(CircuitBreakerState.CLOSED, circuitBreaker.getState());
    }

    @Test
    public void testManualReset() throws Exception {
        Callable<String> callable = () -> {
            throw new RuntimeException("Test exception");
        };
        
        for (int i = 0; i < 3; i++) {
            assertThrows(RuntimeException.class, () -> circuitBreaker.execute(callable));
        }
        
        assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState());
        
        circuitBreaker.reset();
        
        assertEquals(CircuitBreakerState.CLOSED, circuitBreaker.getState());
    }

    @Test
    public void testCircuitBreakerFactory() {
        CircuitBreaker cb1 = CircuitBreakerFactory.create("test-cb-1");
        CircuitBreaker cb2 = CircuitBreakerFactory.create("test-cb-1");
        CircuitBreaker cb3 = CircuitBreakerFactory.create("test-cb-2");
        
        assertSame(cb1, cb2);
        assertNotSame(cb1, cb3);
    }

    @Test
    public void testGetMetrics() throws Exception {
        Callable<String> callable = () -> {
            throw new RuntimeException("Test exception");
        };
        
        for (int i = 0; i < 2; i++) {
            assertThrows(RuntimeException.class, () -> circuitBreaker.execute(callable));
        }
        
        DefaultCircuitBreaker defaultCB = (DefaultCircuitBreaker) circuitBreaker;
        DefaultCircuitBreaker.CircuitBreakerMetrics metrics = defaultCB.getMetrics();
        
        assertEquals(2, metrics.getFailureCount());
        assertEquals(2, metrics.getTotalRequests());
        assertEquals(2, metrics.getTotalFailures());
    }
}
