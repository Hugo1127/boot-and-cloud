package com.bootcloud.concurrent;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LockComparator {
    private final ReentrantLock reentrantLock;
    private final ReadWriteLock readWriteLock;
    private volatile int synchronizedCounter = 0;
    private int reentrantCounter = 0;
    private int readWriteCounter = 0;

    public LockComparator() {
        this.reentrantLock = new ReentrantLock();
        this.readWriteLock = new ReentrantReadWriteLock();
    }

    public void compareLocks(int iterations) {
        System.out.println("=== Lock Performance Comparison ===");
        System.out.println("Running " + iterations + " iterations for each lock type\n");

        long syncTime = testSynchronized(iterations);
        long reentrantTime = testReentrantLock(iterations);
        long rwLockTime = testReadWriteLock(iterations);
        long atomicTime = testAtomic(iterations);

        System.out.println("=== Performance Results ===");
        System.out.println("Synchronized:      " + syncTime + "ms");
        System.out.println("ReentrantLock:      " + reentrantTime + "ms");
        System.out.println("ReadWriteLock:      " + rwLockTime + "ms");
        System.out.println("AtomicInteger:      " + atomicTime + "ms");
        System.out.println("==============================");

        printRecommendations(syncTime, reentrantTime, rwLockTime, atomicTime);
    }

    private long testSynchronized(int iterations) {
        long startTime = System.currentTimeMillis();
        synchronizedCounter = 0;
        
        for (int i = 0; i < iterations; i++) {
            synchronized (this) {
                synchronizedCounter++;
            }
        }
        
        return System.currentTimeMillis() - startTime;
    }

    private long testReentrantLock(int iterations) {
        long startTime = System.currentTimeMillis();
        reentrantCounter = 0;
        
        for (int i = 0; i < iterations; i++) {
            reentrantLock.lock();
            try {
                reentrantCounter++;
            } finally {
                reentrantLock.unlock();
            }
        }
        
        return System.currentTimeMillis() - startTime;
    }

    private long testReadWriteLock(int iterations) {
        long startTime = System.currentTimeMillis();
        readWriteCounter = 0;
        
        for (int i = 0; i < iterations; i++) {
            readWriteLock.writeLock().lock();
            try {
                readWriteCounter++;
            } finally {
                readWriteLock.writeLock().unlock();
            }
        }
        
        return System.currentTimeMillis() - startTime;
    }

    private long testAtomic(int iterations) {
        long startTime = System.currentTimeMillis();
        java.util.concurrent.atomic.AtomicInteger atomicCounter = 
            new java.util.concurrent.atomic.AtomicInteger(0);
        
        for (int i = 0; i < iterations; i++) {
            atomicCounter.incrementAndGet();
        }
        
        return System.currentTimeMillis() - startTime;
    }

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

    private String getPerformanceRating(long time, long baseline) {
        double ratio = (double) time / baseline;
        if (ratio < 0.8) return "Excellent";
        if (ratio < 1.0) return "Good";
        if (ratio < 1.5) return "Fair";
        return "Poor";
    }

    public void demonstrateLockUpgrade() {
        System.out.println("=== Synchronized Lock Upgrade Demonstration ===");
        System.out.println("JVM lock upgrade: Biased -> Lightweight -> Heavyweight");
        
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

        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Lock upgrade demonstration completed");
        System.out.println("Note: Use -XX:+PrintSafepointStatistics -XX:+PrintSafepointStatisticsTimeout=1 to observe lock states");
    }
}
