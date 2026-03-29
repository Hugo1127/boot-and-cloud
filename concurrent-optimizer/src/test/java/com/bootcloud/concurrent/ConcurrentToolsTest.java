package com.bootcloud.concurrent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ConcurrentToolsTest {

    @Test
    public void testDemonstrateCountDownLatch() throws InterruptedException {
        assertDoesNotThrow(() -> ConcurrentTools.demonstrateCountDownLatch(5));
    }

    @Test
    public void testDemonstrateCyclicBarrier() throws InterruptedException {
        assertDoesNotThrow(() -> ConcurrentTools.demonstrateCyclicBarrier(5));
    }

    @Test
    public void testDemonstrateSemaphore() throws InterruptedException {
        assertDoesNotThrow(() -> ConcurrentTools.demonstrateSemaphore(3, 10));
    }

    @Test
    public void testDemonstrateBlockingQueue() throws InterruptedException {
        assertDoesNotThrow(() -> ConcurrentTools.demonstrateBlockingQueue());
    }

    @Test
    public void testCompareQueuePerformance() throws InterruptedException {
        assertDoesNotThrow(() -> ConcurrentTools.compareQueuePerformance(100));
    }
}
