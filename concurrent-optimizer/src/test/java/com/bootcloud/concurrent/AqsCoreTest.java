package com.bootcloud.concurrent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AqsUtils 单元测试
 * 
 * 测试 AQS 框架、ReentrantLock、CountDownLatch
 * 
 * @author BootCloud
 * @date 2026-04-14
 */
public class AqsCoreTest {

    @Test
    @DisplayName("测试 ReentrantLock 的基本加锁解锁")
    public void testReentrantLockBasic() {
        AqsUtils.ReentrantLock lock = new AqsUtils.ReentrantLock();
        
        assertFalse(lock.isLocked());
        
        lock.lock();
        assertTrue(lock.isLocked());
        assertEquals(1, lock.getHoldCount());
        
        lock.unlock();
        assertFalse(lock.isLocked());
        assertEquals(0, lock.getHoldCount());
    }

    @Test
    @DisplayName("测试 ReentrantLock 的可重入性")
    public void testReentrantLockReentrant() {
        AqsUtils.ReentrantLock lock = new AqsUtils.ReentrantLock();
        
        lock.lock();
        assertEquals(1, lock.getHoldCount());
        
        lock.lock();
        assertEquals(2, lock.getHoldCount());
        
        lock.lock();
        assertEquals(3, lock.getHoldCount());
        
        lock.unlock();
        assertEquals(2, lock.getHoldCount());
        
        lock.unlock();
        assertEquals(1, lock.getHoldCount());
        
        lock.unlock();
        assertEquals(0, lock.getHoldCount());
        assertFalse(lock.isLocked());
    }

    @Test
    @DisplayName("测试 ReentrantLock 的 tryLock 方法")
    public void testReentrantLockTryLock() {
        AqsUtils.ReentrantLock lock = new AqsUtils.ReentrantLock();
        
        assertTrue(lock.tryLock());
        assertFalse(lock.tryLock());
        
        lock.unlock();
        assertTrue(lock.tryLock());
        
        lock.unlock();
    }

    @Test
    @DisplayName("测试 ReentrantLock 的线程安全")
    public void testReentrantLockThreadSafety() throws InterruptedException {
        AqsUtils.ReentrantLock lock = new AqsUtils.ReentrantLock();
        AtomicInteger counter = new AtomicInteger(0);
        int threadCount = 10;
        int iterations = 1000;
        
        Thread[] threads = new Thread[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < iterations; j++) {
                    lock.lock();
                    try {
                        counter.incrementAndGet();
                    } finally {
                        lock.unlock();
                    }
                }
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        assertEquals(threadCount * iterations, counter.get(), "计数器应该正确累加");
    }

    @Test
    @DisplayName("测试 CountDownLatch 的基本功能")
    public void testCountDownLatchBasic() throws InterruptedException {
        AqsUtils.CountDownLatch latch = new AqsUtils.CountDownLatch(3);
        
        assertEquals(3, latch.getCount());
        
        latch.countDown();
        assertEquals(2, latch.getCount());
        
        latch.countDown();
        assertEquals(1, latch.getCount());
        
        latch.countDown();
        assertEquals(0, latch.getCount());
    }

    @Test
    @DisplayName("测试 CountDownLatch 的 await 方法")
    public void testCountDownLatchAwait() throws InterruptedException {
        AqsUtils.CountDownLatch latch = new AqsUtils.CountDownLatch(2);
        AtomicInteger completedCount = new AtomicInteger(0);
        
        Thread worker1 = new Thread(() -> {
            try {
                Thread.sleep(100);
                latch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        Thread worker2 = new Thread(() -> {
            try {
                Thread.sleep(200);
                latch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        Thread waiter = new Thread(() -> {
            try {
                latch.await();
                completedCount.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        worker1.start();
        worker2.start();
        waiter.start();
        
        worker1.join();
        worker2.join();
        waiter.join();
        
        assertEquals(0, latch.getCount());
        assertEquals(1, completedCount.get(), "等待线程应该完成");
    }

    @Test
    @DisplayName("测试 CountDownLatch 协调多个线程")
    public void testCountDownLatchCoordination() throws InterruptedException {
        int threadCount = 5;
        AqsUtils.CountDownLatch startLatch = new AqsUtils.CountDownLatch(1);
        AqsUtils.CountDownLatch endLatch = new AqsUtils.CountDownLatch(threadCount);
        AtomicInteger counter = new AtomicInteger(0);
        
        List<Thread> workers = new ArrayList<>();
        
        for (int i = 0; i < threadCount; i++) {
            Thread worker = new Thread(() -> {
                try {
                    startLatch.await();
                    counter.incrementAndGet();
                    Thread.sleep(50);
                    endLatch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            workers.add(worker);
            worker.start();
        }
        
        Thread.sleep(100);
        assertEquals(0, counter.get(), "线程应该在等待启动信号");
        
        startLatch.countDown();
        endLatch.await();
        
        assertEquals(threadCount, counter.get(), "所有线程应该都执行了");
    }

    @Test
    @DisplayName("测试 CountDownLatch 的超时 await")
    public void testCountDownLatchAwaitTimeout() throws InterruptedException {
        AqsUtils.CountDownLatch latch = new AqsUtils.CountDownLatch(1);
        
        long startTime = System.currentTimeMillis();
        boolean completed = latch.await(500, java.util.concurrent.TimeUnit.MILLISECONDS);
        long elapsed = System.currentTimeMillis() - startTime;
        
        assertFalse(completed);
        assertTrue(elapsed >= 400 && elapsed <= 700, "应该在 500ms 左右超时");
    }

    @Test
    @DisplayName("测试 CountDownLatch 的非法参数")
    public void testCountDownLatchInvalidCount() {
        assertThrows(IllegalArgumentException.class, () -> {
            new AqsUtils.CountDownLatch(-1);
        });
    }

    @Test
    @DisplayName("测试 Node 节点的基本功能")
    public void testNodeBasic() {
        Thread thread = Thread.currentThread();
        AqsUtils.Node node = new AqsUtils.Node(thread, AqsUtils.Node.EXCLUSIVE);
        
        assertNotNull(node.thread);
        assertFalse(node.isShared);
        assertFalse(node.isCancelled());
    }

    @Test
    @DisplayName("测试 Node 节点的 predecessor 方法")
    public void testNodePredecessor() {
        AqsUtils.Node node1 = new AqsUtils.Node();
        AqsUtils.Node node2 = new AqsUtils.Node();
        
        node2.prev = node1;
        
        assertSame(node1, node2.predecessor());
    }

    @Test
    @DisplayName("测试 Node 节点的 predecessor 空指针异常")
    public void testNodePredecessorNull() {
        AqsUtils.Node node = new AqsUtils.Node();
        node.prev = null;
        
        assertThrows(NullPointerException.class, () -> {
            node.predecessor();
        });
    }
}
