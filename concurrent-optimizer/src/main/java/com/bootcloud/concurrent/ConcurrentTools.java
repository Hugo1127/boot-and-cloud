package com.bootcloud.concurrent;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Java 并发工具类综合演示
 * 包含：CountDownLatch、CyclicBarrier、Semaphore、BlockingQueue 及队列性能对比
 */
public class ConcurrentTools {
    // 原子计数器，用于统计任务编号（当前示例未直接使用）
    private static final AtomicInteger taskCounter = new AtomicInteger(0);

    /**
     * 演示 CountDownLatch 使用
     * 作用：等待一组线程执行完成后，主线程再继续执行（计数器减到0后唤醒）
     * @param threadCount 需要等待的线程数量
     */
    public static void demonstrateCountDownLatch(int threadCount) throws InterruptedException {
        System.out.println("=== CountDownLatch Demo ===");
        System.out.println("Waiting for " + threadCount + " threads to complete...\n");

        // 初始化计数器，值为线程总数
        CountDownLatch latch = new CountDownLatch(threadCount);
        long startTime = System.currentTimeMillis();

        // 循环创建指定数量的线程
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i + 1;
            new Thread(() -> {
                try {
                    System.out.println("Thread " + threadId + " started");
                    // 模拟线程执行耗时任务
                    Thread.sleep((long) (Math.random() * 1000));
                    System.out.println("Thread " + threadId + " completed");
                } catch (InterruptedException e) {
                    // 恢复中断状态
                    Thread.currentThread().interrupt();
                } finally {
                    // 每个线程执行完毕，计数器减1
                    latch.countDown();
                }
            }).start();
        }

        // 主线程阻塞，直到计数器减为0
        latch.await();
        long elapsedTime = System.currentTimeMillis() - startTime;
        System.out.println("\nAll " + threadCount + " threads completed in " + elapsedTime + "ms");
        System.out.println("=============================");
    }

    /**
     * 演示 CyclicBarrier 使用
     * 作用：让一组线程相互等待，全部到达屏障后再同时继续执行（可循环使用）
     * @param partyCount 参与等待的线程数量
     */
    public static void demonstrateCyclicBarrier(int partyCount) throws InterruptedException {
        System.out.println("=== CyclicBarrier Demo ===");
        System.out.println("Waiting for " + partyCount + " parties to reach barrier...\n");

        // 创建屏障：指定等待线程数 + 所有线程到达后执行的回调任务
        CyclicBarrier barrier = new CyclicBarrier(partyCount, () -> {
            System.out.println("\n=== All parties reached barrier! ===");
            System.out.println("Barrier action executed");
            System.out.println("=============================\n");
        });

        // 创建线程
        for (int i = 0; i < partyCount; i++) {
            final int partyId = i + 1;
            new Thread(() -> {
                try {
                    System.out.println("Party " + partyId + " is working...");
                    // 模拟执行任务
                    Thread.sleep((long) (Math.random() * 1000));
                    System.out.println("Party " + partyId + " arrived at barrier, waiting...");

                    // 线程到达屏障，开始等待其他线程
                    barrier.await();

                    // 所有线程都到达后，继续执行后续代码
                    System.out.println("Party " + partyId + " resumed execution");
                } catch (InterruptedException | BrokenBarrierException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Party " + partyId + " interrupted: " + e.getMessage());
                }
            }).start();
        }

        // 主线程等待所有子线程执行完毕
        Thread.sleep(3000);
        System.out.println("CyclicBarrier demo completed");
        System.out.println("=============================");
    }

    /**
     * 演示 Semaphore 使用
     * 作用：控制同时访问资源的线程数量（信号量，限流）
     * @param permits 允许同时执行的线程数（许可证数量）
     * @param threadCount 总任务线程数
     */
    public static void demonstrateSemaphore(int permits, int threadCount) throws InterruptedException {
        System.out.println("=== Semaphore Demo ===");
        System.out.println("Permits: " + permits + ", Threads: " + threadCount + "\n");

        // 初始化信号量，指定许可证数量
        Semaphore semaphore = new Semaphore(permits);
        // 原子统计：当前正在执行的任务数
        AtomicInteger activeTasks = new AtomicInteger(0);
        // 原子统计：已完成的任务数
        AtomicInteger completedTasks = new AtomicInteger(0);

        // 用于等待所有任务执行完成
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int taskId = i + 1;
            new Thread(() -> {
                try {
                    System.out.println("Task " + taskId + " waiting for permit...");
                    // 获取许可证，无许可证则阻塞
                    semaphore.acquire();

                    // 任务开始执行
                    int active = activeTasks.incrementAndGet();
                    System.out.println("Task " + taskId + " acquired permit. Active tasks: " + active);

                    // 模拟任务执行
                    Thread.sleep((long) (Math.random() * 1000));

                    // 任务执行完成
                    activeTasks.decrementAndGet();
                    System.out.println("Task " + taskId + " completed. Active tasks: " + activeTasks.get());
                    completedTasks.incrementAndGet();

                    // 释放许可证，让其他等待线程可以获取
                    semaphore.release();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        // 等待所有任务执行完毕
        latch.await();
        System.out.println("\nAll " + threadCount + " tasks completed");
        System.out.println("Semaphore demo completed");
        System.out.println("=============================");
    }

    /**
     * 演示 BlockingQueue 使用
     * 作用：线程安全的阻塞队列，生产者消费者模型核心组件
     */
    public static void demonstrateBlockingQueue() throws InterruptedException {
        System.out.println("=== BlockingQueue Demo ===");

        // 基于链表的阻塞队列，容量3
        BlockingQueue<String> queue = new LinkedBlockingQueue<>(3);
        // 生产/消费计数器
        AtomicInteger producedCount = new AtomicInteger(0);
        AtomicInteger consumedCount = new AtomicInteger(0);

        // 生产者线程：生产10个元素放入队列
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    String item = "Item-" + i;
                    System.out.println("Producing: " + item);
                    // 队列满时阻塞等待
                    queue.put(item);
                    System.out.println("Produced: " + item + " (Queue size: " + queue.size() + ")");
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 消费者线程：从队列取10个元素
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    // 队列空时阻塞等待
                    String item = queue.take();
                    System.out.println("Consumed: " + item + " (Queue size: " + queue.size() + ")");
                    Thread.sleep(700);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 启动线程
        producer.start();
        consumer.start();

        // 等待线程执行完成
        producer.join();
        consumer.join();

        System.out.println("\nBlockingQueue demo completed");
        System.out.println("=============================");
    }

    /**
     * 两种阻塞队列性能对比
     * LinkedBlockingQueue vs ArrayBlockingQueue
     * @param items 测试的元素数量
     */
    public static void compareQueuePerformance(int items) throws InterruptedException {
        System.out.println("=== Queue Performance Comparison ===");
        System.out.println("Processing " + items + " items\n");

        // -------- 测试 LinkedBlockingQueue --------
        CountDownLatch latch1 = new CountDownLatch(items);
        BlockingQueue<Integer> linkedBlockingQueue = new LinkedBlockingQueue<>(100);
        long start1 = System.currentTimeMillis();

        for (int i = 0; i < items; i++) {
            final int value = i;
            new Thread(() -> {
                try {
                    // 入队 + 出队
                    linkedBlockingQueue.put(value);
                    linkedBlockingQueue.take();
                    latch1.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
        latch1.await();
        long time1 = System.currentTimeMillis() - start1;

        // -------- 测试 ArrayBlockingQueue --------
        CountDownLatch latch2 = new CountDownLatch(items);
        BlockingQueue<Integer> arrayBlockingQueue = new ArrayBlockingQueue<>(100);
        long start2 = System.currentTimeMillis();

        for (int i = 0; i < items; i++) {
            final int value = i;
            new Thread(() -> {
                try {
                    arrayBlockingQueue.put(value);
                    arrayBlockingQueue.take();
                    latch2.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
        latch2.await();
        long time2 = System.currentTimeMillis() - start2;

        // 输出性能对比结果
        System.out.println("=== Performance Results ===");
        System.out.println("LinkedBlockingQueue: " + time1 + "ms");
        System.out.println("ArrayBlockingQueue:  " + time2 + "ms");
        System.out.println("Difference: " + Math.abs(time1 - time2) + "ms");
        System.out.println("==============================");

        // 使用建议总结
        System.out.println("\nRecommendation:");
        System.out.println("Use LinkedBlockingQueue for:");
        System.out.println("  - Unbounded queues (when set to Integer.MAX_VALUE)");
        System.out.println("  - High throughput scenarios");
        System.out.println("  - Dynamic size requirements");
        System.out.println("\nUse ArrayBlockingQueue for:");
        System.out.println("  - Bounded queues with fixed size");
        System.out.println("  - Better memory locality");
        System.out.println("  - Predictable memory usage");
    }
}