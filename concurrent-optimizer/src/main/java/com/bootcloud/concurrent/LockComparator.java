package com.bootcloud.concurrent;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Java 各种锁机制性能对比工具类
 * 对比对象：synchronized、ReentrantLock、ReadWriteLock、AtomicInteger
 * 功能：性能测试 + 使用建议 + synchronized 锁升级演示
 */
public class LockComparator {
    // 可重入锁（显式锁）
    private final ReentrantLock reentrantLock;
    // 读写锁（读共享、写独占）
    private final ReadWriteLock readWriteLock;

    // volatile 保证线程可见性，用于 synchronized 测试计数器
    private volatile int synchronizedCounter = 0;
    // ReentrantLock 测试计数器
    private int reentrantCounter = 0;
    // ReadWriteLock 测试计数器
    private int readWriteCounter = 0;

    /**
     * 构造方法：初始化锁对象
     */
    public LockComparator() {
        // 初始化非公平可重入锁
        this.reentrantLock = new ReentrantLock();
        // 初始化读写锁
        this.readWriteLock = new ReentrantReadWriteLock();
    }

    /**
     * 统一入口：执行所有锁的性能对比测试
     * @param iterations 每个锁的执行次数
     */
    public void compareLocks(int iterations) {
        System.out.println("=== Lock Performance Comparison ===");
        System.out.println("Running " + iterations + " iterations for each lock type\n");

        // 分别测试四种锁/原子类的执行耗时
        long syncTime = testSynchronized(iterations);
        long reentrantTime = testReentrantLock(iterations);
        long rwLockTime = testReadWriteLock(iterations);
        long atomicTime = testAtomic(iterations);

        // 输出性能结果
        System.out.println("=== Performance Results ===");
        System.out.println("Synchronized:      " + syncTime + "ms");
        System.out.println("ReentrantLock:      " + reentrantTime + "ms");
        System.out.println("ReadWriteLock:      " + rwLockTime + "ms");
        System.out.println("AtomicInteger:      " + atomicTime + "ms");
        System.out.println("==============================");

        // 输出使用建议
        printRecommendations(syncTime, reentrantTime, rwLockTime, atomicTime);
    }

    /**
     * 测试 synchronized 关键字性能
     * 特点：JVM 内置锁，自动加锁、自动释放
     */
    private long testSynchronized(int iterations) {
        long startTime = System.currentTimeMillis();
        synchronizedCounter = 0;

        for (int i = 0; i < iterations; i++) {
            // 加锁：当前对象作为锁对象
            synchronized (this) {
                synchronizedCounter++;
            }
            // 同步块结束：JVM 自动释放锁
        }

        return System.currentTimeMillis() - startTime;
    }

    /**
     * 测试 ReentrantLock 可重入锁性能
     * 特点：手动加锁/解锁，支持超时、中断、公平锁
     * 注意：必须在 finally 中 unlock，否则会造成死锁
     */
    private long testReentrantLock(int iterations) {
        long startTime = System.currentTimeMillis();
        reentrantCounter = 0;

        for (int i = 0; i < iterations; i++) {
            // 手动加锁
            reentrantLock.lock();
            try {
                reentrantCounter++;
            } finally {
                // 手动释放锁（必须放 finally）
                reentrantLock.unlock();
            }
        }

        return System.currentTimeMillis() - startTime;
    }

    /**
     * 测试 ReadWriteLock 读写锁性能
     * 本方法使用写锁（独占锁），所有线程互斥
     * 适合场景：读多写少
     */
    private long testReadWriteLock(int iterations) {
        long startTime = System.currentTimeMillis();
        readWriteCounter = 0;

        for (int i = 0; i < iterations; i++) {
            // 获取写锁（独占锁，所有线程互斥）
            readWriteLock.writeLock().lock();
            try {
                readWriteCounter++;
            } finally {
                // 释放写锁
                readWriteLock.writeLock().unlock();
            }
        }

        return System.currentTimeMillis() - startTime;
    }

    /**
     * 测试 AtomicInteger 原子类性能（CAS 无锁机制）
     * 特点：无锁、轻量、靠 CPU 指令保证原子性
     * 适合：简单变量的原子更新
     */
    private long testAtomic(int iterations) {
        long startTime = System.currentTimeMillis();
        // 初始化原子整数
        java.util.concurrent.atomic.AtomicInteger atomicCounter =
                new java.util.concurrent.atomic.AtomicInteger(0);

        for (int i = 0; i < iterations; i++) {
            // 原子自增（无锁操作）
            atomicCounter.incrementAndGet();
        }

        return System.currentTimeMillis() - startTime;
    }

    /**
     * 输出四种锁的使用建议、优缺点、性能评级
     */
    private void printRecommendations(long syncTime, long reentrantTime,
                                      long rwLockTime, long atomicTime) {
        System.out.println("\n=== Lock Usage Recommendations ===");

        System.out.println("Synchronized:");
        System.out.println("  - Use when: Simple synchronization needed");
        System.out.println("  - Advantages: JVM optimizes (lock elision, biased locking)");
        System.out.println("  - Disadvantages: No timeout, no fairness control");
        System.out.println("  - Performance: " + getPerformanceRating(syncTime, syncTime));

        System.out.println("\nReentrantLock:");
        System.out.println("  - Use when: Advanced features needed (timeout, fairness, interrupt)");
        System.out.println("  - Advantages: tryLock, timeout, interruptible, fair lock");
        System.out.println("  - Disadvantages: Manual unlock required in finally");
        System.out.println("  - Performance: " + getPerformanceRating(reentrantTime, syncTime));

        System.out.println("\nReadWriteLock:");
        System.out.println("  - Use when: High read-to-write ratio (>10:1)");
        System.out.println("  - Advantages: Multiple readers can read concurrently");
        System.out.println("  - Disadvantages: Write lock blocks all readers");
        System.out.println("  - Performance: " + getPerformanceRating(rwLockTime, syncTime));

        System.out.println("\nAtomicInteger (CAS):");
        System.out.println("  - Use when: Single variable updates, low contention");
        System.out.println("  - Advantages: Lock-free, high performance");
        System.out.println("  - Disadvantages: ABA problem, limited to simple operations");
        System.out.println("  - Performance: " + getPerformanceRating(atomicTime, syncTime));
        System.out.println("===============================");
    }

    /**
     * 性能评级工具方法
     * 以 synchronized 为基准，对比其他锁的速度
     */
    private String getPerformanceRating(long time, long baseline) {
        double ratio = (double) time / baseline;
        if (ratio < 0.8) return "Excellent";
        if (ratio < 1.0) return "Good";
        if (ratio < 1.5) return "Fair";
        return "Poor";
    }

    /**
     * 演示 synchronized 锁升级机制
     * JVM 会根据竞争情况自动升级：
     * 偏向锁 → 轻量级锁（自旋锁） → 重量级锁（OS 互斥锁）
     */
    public void demonstrateLockUpgrade() {
        System.out.println("=== Synchronized Lock Upgrade Demonstration ===");
        System.out.println("JVM lock upgrade: Biased -> Lightweight -> Heavyweight");

        // 线程1：频繁竞争锁，触发锁升级
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                synchronized (this) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });

        // 线程2：与线程1 竞争同一把锁，加剧竞争
        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                synchronized (this) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });

        thread1.start();
        thread2.start();

        // 主线程等待两个子线程执行完毕
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Lock upgrade demonstration completed");
        // JVM 参数，用于观察锁状态（面试/调试常用）
        System.out.println("Note: Use -XX:+PrintSafepointStatistics -XX:+PrintSafepointStatisticsTimeout=1 to observe lock states");
    }
}