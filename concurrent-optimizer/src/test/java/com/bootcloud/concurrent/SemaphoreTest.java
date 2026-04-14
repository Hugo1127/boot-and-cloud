package com.bootcloud.concurrent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SemaphoreUtils 单元测试
 * 
 * 测试 Semaphore 信号量的功能和场景
 * 
 * @author BootCloud
 * @date 2026-04-14
 */
public class SemaphoreTest {

    @Test
    @DisplayName("测试 Semaphore 的基本获取和释放")
    public void testSemaphoreBasic() throws InterruptedException {
        SemaphoreUtils.Semaphore semaphore = new SemaphoreUtils.Semaphore(3);
        
        assertEquals(3, semaphore.availablePermits());
        
        semaphore.acquire();
        assertEquals(2, semaphore.availablePermits());
        
        semaphore.acquire();
        assertEquals(1, semaphore.availablePermits());
        
        semaphore.acquire();
        assertEquals(0, semaphore.availablePermits());
        
        semaphore.release();
        assertEquals(1, semaphore.availablePermits());
        
        semaphore.release();
        assertEquals(2, semaphore.availablePermits());
        
        semaphore.release();
        assertEquals(3, semaphore.availablePermits());
    }

    @Test
    @DisplayName("测试 Semaphore 获取多个许可")
    public void testSemaphoreAcquireMultiple() throws InterruptedException {
        SemaphoreUtils.Semaphore semaphore = new SemaphoreUtils.Semaphore(5);
        
        semaphore.acquire(2);
        assertEquals(3, semaphore.availablePermits());
        
        semaphore.acquire(2);
        assertEquals(1, semaphore.availablePermits());
        
        semaphore.release(2);
        assertEquals(3, semaphore.availablePermits());
        
        semaphore.release(2);
        assertEquals(5, semaphore.availablePermits());
    }

    @Test
    @DisplayName("测试 Semaphore 的 tryAcquire 方法")
    public void testSemaphoreTryAcquire() {
        SemaphoreUtils.Semaphore semaphore = new SemaphoreUtils.Semaphore(2);
        
        assertTrue(semaphore.tryAcquire());
        assertTrue(semaphore.tryAcquire());
        assertFalse(semaphore.tryAcquire());
        
        semaphore.release();
        assertTrue(semaphore.tryAcquire());
        
        semaphore.release(2);
        assertTrue(semaphore.tryAcquire());
    }

    @Test
    @DisplayName("测试 Semaphore 的限流功能")
    public void testSemaphoreRateLimiting() throws InterruptedException {
        SemaphoreUtils.Semaphore semaphore = new SemaphoreUtils.Semaphore(3);
        AtomicInteger maxConcurrent = new AtomicInteger(0);
        AtomicInteger currentConcurrent = new AtomicInteger(0);
        int threadCount = 10;
        
        List<Thread> threads = new ArrayList<>();
        
        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> {
                try {
                    semaphore.acquire();
                    int current = currentConcurrent.incrementAndGet();
                    if (current > maxConcurrent.get()) {
                        maxConcurrent.set(current);
                    }
                    
                    Thread.sleep(100);
                    
                    currentConcurrent.decrementAndGet();
                    semaphore.release();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            threads.add(thread);
            thread.start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        assertTrue(maxConcurrent.get() <= 3, "最大并发数不应超过 3");
        assertEquals(0, currentConcurrent.get(), "所有线程应该已完成");
    }

    @Test
    @DisplayName("测试 Semaphore 控制数据库连接池场景")
    public void testSemaphoreDatabasePool() throws InterruptedException {
        int poolSize = 3;
        SemaphoreUtils.Semaphore semaphore = new SemaphoreUtils.Semaphore(poolSize);
        AtomicInteger activeConnections = new AtomicInteger(0);
        AtomicInteger maxConnections = new AtomicInteger(0);
        int queryCount = 6;
        
        List<Thread> queries = new ArrayList<>();
        
        for (int i = 0; i < queryCount; i++) {
            Thread query = new Thread(() -> {
                try {
                    semaphore.acquire();
                    int current = activeConnections.incrementAndGet();
                    if (current > maxConnections.get()) {
                        maxConnections.set(current);
                    }
                    
                    Thread.sleep(100);
                    
                    activeConnections.decrementAndGet();
                    semaphore.release();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            queries.add(query);
            query.start();
        }
        
        for (Thread query : queries) {
            query.join();
        }
        
        assertTrue(maxConnections.get() <= poolSize, "最大连接数不应超过池大小");
        assertEquals(0, activeConnections.get(), "所有连接应该已释放");
    }

    @Test
    @DisplayName("测试 Semaphore 实现生产者消费者模式")
    public void testSemaphoreProducerConsumer() throws InterruptedException {
        SemaphoreUtils.Semaphore emptyPermits = new SemaphoreUtils.Semaphore(10);
        SemaphoreUtils.Semaphore filledPermits = new SemaphoreUtils.Semaphore(0);
        AtomicInteger produced = new AtomicInteger(0);
        AtomicInteger consumed = new AtomicInteger(0);
        
        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    emptyPermits.acquire();
                    produced.incrementAndGet();
                    Thread.sleep(50);
                    filledPermits.release();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    filledPermits.acquire();
                    consumed.incrementAndGet();
                    Thread.sleep(80);
                    emptyPermits.release();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        producer.start();
        consumer.start();
        
        producer.join();
        consumer.join();
        
        assertEquals(5, produced.get(), "应该生产 5 个产品");
        assertEquals(5, consumed.get(), "应该消费 5 个产品");
    }

    @Test
    @DisplayName("测试 Semaphore 的非法参数")
    public void testSemaphoreInvalidPermits() {
        assertThrows(IllegalArgumentException.class, () -> {
            new SemaphoreUtils.Semaphore(-1);
        });
    }

    @Test
    @DisplayName("测试 Semaphore 的 acquireUninterruptibly 方法")
    public void testSemaphoreAcquireUninterruptibly() {
        SemaphoreUtils.Semaphore semaphore = new SemaphoreUtils.Semaphore(1);
        
        semaphore.acquireUninterruptibly(1);
        assertEquals(0, semaphore.availablePermits());
        
        semaphore.release();
        assertEquals(1, semaphore.availablePermits());
    }

    @Test
    @DisplayName("测试 Semaphore 多线程并发获取")
    public void testSemaphoreMultiThreadedAcquire() throws InterruptedException {
        SemaphoreUtils.Semaphore semaphore = new SemaphoreUtils.Semaphore(5);
        AtomicInteger acquiredCount = new AtomicInteger(0);
        int threadCount = 20;
        
        List<Thread> threads = new ArrayList<>();
        
        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> {
                try {
                    semaphore.acquire();
                    acquiredCount.incrementAndGet();
                    Thread.sleep(50);
                    semaphore.release();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            threads.add(thread);
            thread.start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        assertEquals(threadCount, acquiredCount.get(), "所有线程都应该获取到许可");
        assertEquals(5, semaphore.availablePermits(), "所有许可应该已释放");
    }

    @Test
    @DisplayName("测试 Semaphore 的超时 tryAcquire")
    public void testSemaphoreTryAcquireTimeout() throws InterruptedException {
        SemaphoreUtils.Semaphore semaphore = new SemaphoreUtils.Semaphore(0);
        
        long startTime = System.currentTimeMillis();
        boolean acquired = semaphore.tryAcquire(500, java.util.concurrent.TimeUnit.MILLISECONDS);
        long elapsed = System.currentTimeMillis() - startTime;
        
        assertFalse(acquired);
        assertTrue(elapsed >= 400 && elapsed <= 700, "应该在 500ms 左右超时");
    }
}
