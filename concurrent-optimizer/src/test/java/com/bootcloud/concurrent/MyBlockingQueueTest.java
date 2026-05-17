package com.bootcloud.concurrent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HandWrittenBlockingQueue 单元测试
 */
@DisplayName("HandWrittenBlockingQueue")
class MyBlockingQueueTest {

    private MyBlockingQueue<String> queue;

    @BeforeEach
    void setUp() {
        queue = new MyBlockingQueue<>(5);
    }

    // ========== 构造与基础测试 ==========

    @Test
    @DisplayName("非法容量应抛异常")
    void shouldRejectInvalidCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new MyBlockingQueue<>(0));
        assertThrows(IllegalArgumentException.class, () -> new MyBlockingQueue<>(-1));
    }

    @Test
    @DisplayName("初始状态正确")
    void shouldStartEmpty() {
        assertEquals(0, queue.size());
        assertTrue(queue.isEmpty());
        assertFalse(queue.isFull());
        assertEquals(5, queue.getCapacity());
        assertNull(queue.peek());
    }

    @Test
    @DisplayName("toString 不抛异常")
    void toStringShouldWork() {
        assertNotNull(queue.toString());
    }

    // ========== offer / poll（非阻塞）==========

    @Test
    @DisplayName("offer 和 poll 基本功能")
    void offerAndPoll() {
        assertTrue(queue.offer("A"));
        assertTrue(queue.offer("B"));
        assertEquals(2, queue.size());
        assertEquals("A", queue.poll());
        assertEquals("B", queue.poll());
        assertNull(queue.poll());
    }

    @Test
    @DisplayName("队列满时 offer 返回 false")
    void offerShouldReturnFalseWhenFull() {
        for (int i = 0; i < 5; i++) {
            assertTrue(queue.offer("item-" + i));
        }
        assertFalse(queue.offer("overflow"));
        assertTrue(queue.isFull());
    }

    @Test
    @DisplayName("空队列 poll 返回 null")
    void pollShouldReturnNullWhenEmpty() {
        assertNull(queue.poll());
    }

    @Test
    @DisplayName("peek 不改变队列状态")
    void peekShouldNotRemove() {
        assertTrue(queue.offer("X"));
        assertEquals("X", queue.peek());
        assertEquals("X", queue.peek());
        assertEquals(1, queue.size());
    }

    // ========== put / take（阻塞）==========

    @Test
    @DisplayName("put null 应抛 NPE")
    void putNullShouldThrowNPE() {
        assertThrows(NullPointerException.class, () -> queue.put(null));
    }

    @Test
    @DisplayName("offer null 应抛 NPE")
    void offerNullShouldThrowNPE() {
        assertThrows(NullPointerException.class, () -> queue.offer(null));
    }

    @Test
    @DisplayName("put 和 take 基本功能")
    void putAndTake() throws InterruptedException {
        queue.put("hello");
        assertEquals("hello", queue.take());
        assertEquals(0, queue.size());
    }

    @Test
    @DisplayName("put 满时应阻塞，take 后继续")
    void putShouldBlockWhenFull() throws InterruptedException {
        // 填满队列
        for (int i = 0; i < 5; i++) {
            queue.put("item-" + i);
        }
        assertTrue(queue.isFull());

        // 消费者线程：消费一个元素
        Thread consumer = new Thread(() -> {
            try {
                Thread.sleep(100);
                queue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        consumer.start();

        // 生产者：put 第 6 个元素，应该被阻塞，直到消费者消费后
        long start = System.currentTimeMillis();
        AtomicBoolean putDone = new AtomicBoolean(false);
        Thread producer = new Thread(() -> {
            try {
                queue.put("item-5");
                putDone.set(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        producer.start();
        producer.join(3000);
        consumer.join(3000);

        assertTrue(putDone.get(), "put 应在消费者 take 后完成");
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed >= 100, "put 被阻塞了一段时间");
    }

    @Test
    @DisplayName("take 空时应阻塞，put 后继续")
    void takeShouldBlockWhenEmpty() throws InterruptedException {
        AtomicReference<String> taken = new AtomicReference<>();
        Thread consumer = new Thread(() -> {
            try {
                taken.set(queue.take());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        consumer.start();

        // 等待消费者开始阻塞
        Thread.sleep(100);

        // 生产者 put 一个元素
        queue.put("rescue");
        consumer.join(3000);

        assertEquals("rescue", taken.get());
    }

    // ========== 超时变体 ==========

    @Test
    @DisplayName("offer 超时返回 false")
    void putTimeout() throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            queue.put("item-" + i);
        }
        long start = System.currentTimeMillis();
        assertFalse(queue.offer("overflow", 100, TimeUnit.MILLISECONDS));
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed >= 100, "应该等待了至少 100ms");
    }

    @Test
    @DisplayName("poll 超时返回 null")
    void pollTimeout() throws InterruptedException {
        assertNull(queue.poll(100, TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("put 在超时时间内成功")
    void putWithinTimeout() throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            queue.put("item-" + i);
        }
        // 消费线程会在 50ms 后消费一个元素
        Thread consumer = new Thread(() -> {
            try {
                Thread.sleep(50);
                queue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        consumer.start();

        assertTrue(queue.offer("late-item", 500, TimeUnit.MILLISECONDS));
        consumer.join(1000);
    }

    // ========== clear ==========

    @Test
    @DisplayName("clear 清空队列")
    void clear() throws InterruptedException {
        queue.put("A");
        queue.put("B");
        queue.clear();
        assertEquals(0, queue.size());
        assertTrue(queue.isEmpty());
    }

    // ========== 生产者消费者模型 ==========

    @Test
    @DisplayName("生产者消费者正确性")
    void producerConsumerCorrectness() throws InterruptedException {
        int itemCount = 100;
        MyBlockingQueue<Integer> q = new MyBlockingQueue<>(10);
        AtomicInteger sum = new AtomicInteger(0);

        // 生产者
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= itemCount; i++) {
                    q.put(i);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 消费者
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < itemCount; i++) {
                    int val = q.take();
                    sum.addAndGet(val);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();
        consumer.start();
        producer.join(5000);
        consumer.join(5000);

        // 1+2+...+100 = 5050
        assertEquals(5050, sum.get());
    }

    // ========== 并发安全测试 ==========

    @Test
    @DisplayName("多线程并发 offer/poll 正确性")
    void concurrentOfferPoll() throws InterruptedException {
        int threadCount = 20;
        int perThread = 1000;
        MyBlockingQueue<Integer> q = new MyBlockingQueue<>(50);
        AtomicInteger totalProduced = new AtomicInteger(0);
        AtomicInteger totalConsumed = new AtomicInteger(0);

        List<Thread> producers = new ArrayList<>();
        List<Thread> consumers = new ArrayList<>();

        // 启动生产者
        for (int t = 0; t < threadCount; t++) {
            Thread th = new Thread(() -> {
                for (int i = 0; i < perThread; i++) {
                    while (!q.offer(i)) {
                        // 队列满，稍后重试
                        try { Thread.sleep(1); } catch (InterruptedException e) { return; }
                    }
                    totalProduced.incrementAndGet();
                }
            });
            producers.add(th);
            th.start();
        }

        // 启动消费者
        int expected = threadCount * perThread;
        for (int t = 0; t < threadCount; t++) {
            Thread th = new Thread(() -> {
                while (totalConsumed.get() < expected) {
                    Integer val = q.poll();
                    if (val != null) {
                        totalConsumed.incrementAndGet();
                    } else {
                        try { Thread.sleep(1); } catch (InterruptedException e) { return; }
                    }
                }
            });
            consumers.add(th);
            th.start();
        }

        for (Thread p : producers) p.join(10000);
        for (Thread c : consumers) c.join(10000);

        assertEquals(expected, totalProduced.get());
        assertEquals(expected, totalConsumed.get());
    }
}