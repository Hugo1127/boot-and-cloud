package com.bootcloud.concurrent;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class SmartThreadPoolTest {

    @Test
    public void testCreateCpuIntensivePool() {
        SmartThreadPool pool = SmartThreadPool.createCpuIntensivePool("test-cpu-pool");
        assertNotNull(pool);
        assertTrue(pool.getCorePoolSize() >= 1);
        pool.shutdown();
    }

    @Test
    public void testCreateIoIntensivePool() {
        SmartThreadPool pool = SmartThreadPool.createIoIntensivePool("test-io-pool");
        assertNotNull(pool);
        assertTrue(pool.getCorePoolSize() >= 2);
        pool.shutdown();
    }

    @Test
    public void testTaskExecution() throws InterruptedException {
        SmartThreadPool pool = SmartThreadPool.createCpuIntensivePool("test-execution-pool");
        CountDownLatch latch = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            pool.execute(() -> {
                try {
                    Thread.sleep(100);
                    latch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        pool.shutdown();
    }

    @Test
    public void testPrintStatistics() {
        SmartThreadPool pool = SmartThreadPool.createCpuIntensivePool("test-stats-pool");
        assertDoesNotThrow(() -> pool.printStatistics());
        pool.shutdown();
    }

    @Test
    public void testDynamicResize() {
        SmartThreadPool pool = SmartThreadPool.createCpuIntensivePool("test-resize-pool");
        int originalCoreSize = pool.getCorePoolSize();
        
        pool.dynamicResize(originalCoreSize + 2, originalCoreSize + 4);
        assertEquals(originalCoreSize + 2, pool.getCorePoolSize());
        assertEquals(originalCoreSize + 4, pool.getMaximumPoolSize());
        
        pool.shutdown();
    }

    @Test
    public void testRejectionHandler() throws InterruptedException {
        SmartThreadPool pool = new SmartThreadPool(1, 1, 60, TimeUnit.SECONDS, 
            new java.util.concurrent.LinkedBlockingQueue<>(1), "test-rejection-pool");
        CountDownLatch latch = new CountDownLatch(3);

        for (int i = 0; i < 3; i++) {
            final int taskId = i;
            pool.execute(() -> {
                try {
                    Thread.sleep(1000);
                    latch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        pool.shutdown();
    }
}
