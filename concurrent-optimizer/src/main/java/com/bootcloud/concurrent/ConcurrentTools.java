package com.bootcloud.concurrent;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

public class ConcurrentTools {
    private static final AtomicInteger taskCounter = new AtomicInteger(0);

    public static void demonstrateCountDownLatch(int threadCount) throws InterruptedException {
        System.out.println("=== CountDownLatch Demo ===");
        System.out.println("Waiting for " + threadCount + " threads to complete...\n");

        CountDownLatch latch = new CountDownLatch(threadCount);
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i + 1;
            new Thread(() -> {
                try {
                    System.out.println("Thread " + threadId + " started");
                    Thread.sleep((long) (Math.random() * 1000));
                    System.out.println("Thread " + threadId + " completed");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
        long elapsedTime = System.currentTimeMillis() - startTime;
        System.out.println("\nAll " + threadCount + " threads completed in " + elapsedTime + "ms");
        System.out.println("=============================");
    }

    public static void demonstrateCyclicBarrier(int partyCount) throws InterruptedException {
        System.out.println("=== CyclicBarrier Demo ===");
        System.out.println("Waiting for " + partyCount + " parties to reach barrier...\n");

        CyclicBarrier barrier = new CyclicBarrier(partyCount, () -> {
            System.out.println("\n=== All parties reached barrier! ===");
            System.out.println("Barrier action executed");
            System.out.println("=============================\n");
        });

        for (int i = 0; i < partyCount; i++) {
            final int partyId = i + 1;
            new Thread(() -> {
                try {
                    System.out.println("Party " + partyId + " is working...");
                    Thread.sleep((long) (Math.random() * 1000));
                    System.out.println("Party " + partyId + " arrived at barrier, waiting...");
                    barrier.await();
                    System.out.println("Party " + partyId + " resumed execution");
                } catch (InterruptedException | BrokenBarrierException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Party " + partyId + " interrupted: " + e.getMessage());
                }
            }).start();
        }

        Thread.sleep(3000);
        System.out.println("CyclicBarrier demo completed");
        System.out.println("=============================");
    }

    public static void demonstrateSemaphore(int permits, int threadCount) throws InterruptedException {
        System.out.println("=== Semaphore Demo ===");
        System.out.println("Permits: " + permits + ", Threads: " + threadCount + "\n");

        Semaphore semaphore = new Semaphore(permits);
        AtomicInteger activeTasks = new AtomicInteger(0);
        AtomicInteger completedTasks = new AtomicInteger(0);

        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int taskId = i + 1;
            new Thread(() -> {
                try {
                    System.out.println("Task " + taskId + " waiting for permit...");
                    semaphore.acquire();
                    
                    int active = activeTasks.incrementAndGet();
                    System.out.println("Task " + taskId + " acquired permit. Active tasks: " + active);
                    
                    Thread.sleep((long) (Math.random() * 1000));
                    
                    activeTasks.decrementAndGet();
                    System.out.println("Task " + taskId + " completed. Active tasks: " + activeTasks.get());
                    completedTasks.incrementAndGet();
                    
                    semaphore.release();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
        System.out.println("\nAll " + threadCount + " tasks completed");
        System.out.println("Semaphore demo completed");
        System.out.println("=============================");
    }

    public static void demonstrateBlockingQueue() throws InterruptedException {
        System.out.println("=== BlockingQueue Demo ===");
        
        BlockingQueue<String> queue = new LinkedBlockingQueue<>(3);
        AtomicInteger producedCount = new AtomicInteger(0);
        AtomicInteger consumedCount = new AtomicInteger(0);

        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    String item = "Item-" + i;
                    System.out.println("Producing: " + item);
                    queue.put(item);
                    System.out.println("Produced: " + item + " (Queue size: " + queue.size() + ")");
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    String item = queue.take();
                    System.out.println("Consumed: " + item + " (Queue size: " + queue.size() + ")");
                    Thread.sleep(700);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();
        consumer.start();

        producer.join();
        consumer.join();

        System.out.println("\nBlockingQueue demo completed");
        System.out.println("=============================");
    }

    public static void compareQueuePerformance(int items) throws InterruptedException {
        System.out.println("=== Queue Performance Comparison ===");
        System.out.println("Processing " + items + " items\n");

        CountDownLatch latch1 = new CountDownLatch(items);
        BlockingQueue<Integer> linkedBlockingQueue = new LinkedBlockingQueue<>(100);
        long start1 = System.currentTimeMillis();
        
        for (int i = 0; i < items; i++) {
            final int value = i;
            new Thread(() -> {
                try {
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

        System.out.println("=== Performance Results ===");
        System.out.println("LinkedBlockingQueue: " + time1 + "ms");
        System.out.println("ArrayBlockingQueue:  " + time2 + "ms");
        System.out.println("Difference: " + Math.abs(time1 - time2) + "ms");
        System.out.println("==============================");

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
