package com.bootcloud.concurrent;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class SmartThreadPool extends ThreadPoolExecutor {
    private final String poolName;
    private final AtomicLong completedTasks;
    private final AtomicInteger activeThreads;
    private final long startTime;

    public SmartThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, 
                         TimeUnit unit, BlockingQueue<Runnable> workQueue, String poolName) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, 
              new BootCloudThreadFactory(poolName), new BootCloudRejectedExecutionHandler());
        this.poolName = poolName;
        this.completedTasks = new AtomicLong(0);
        this.activeThreads = new AtomicInteger(0);
        this.startTime = System.currentTimeMillis();
    }

    public static SmartThreadPool createCpuIntensivePool(String poolName) {
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        int maxPoolSize = Runtime.getRuntime().availableProcessors() + 1;
        return new SmartThreadPool(
            corePoolSize,
            maxPoolSize,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            poolName
        );
    }

    public static SmartThreadPool createIoIntensivePool(String poolName) {
        int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;
        int maxPoolSize = Runtime.getRuntime().availableProcessors() * 4;
        return new SmartThreadPool(
            corePoolSize,
            maxPoolSize,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            poolName
        );
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        activeThreads.incrementAndGet();
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        activeThreads.decrementAndGet();
        completedTasks.incrementAndGet();
        
        if (t != null) {
            System.err.println("Task execution failed: " + t.getMessage());
            t.printStackTrace();
        }
    }

    public void printStatistics() {
        long uptime = System.currentTimeMillis() - startTime;
        double avgTasksPerSecond = completedTasks.get() * 1000.0 / uptime;
        
        System.out.println("=== Thread Pool Statistics [" + poolName + "] ===");
        System.out.println("Core Pool Size: " + getCorePoolSize());
        System.out.println("Max Pool Size: " + getMaximumPoolSize());
        System.out.println("Current Pool Size: " + getPoolSize());
        System.out.println("Active Threads: " + getActiveCount());
        System.out.println("Queue Size: " + getQueue().size());
        System.out.println("Completed Tasks: " + getCompletedTaskCount());
        System.out.println("Total Tasks: " + getTaskCount());
        System.out.println("Average Tasks/Second: " + String.format("%.2f", avgTasksPerSecond));
        System.out.println("Uptime: " + (uptime / 1000.0) + "s");
        System.out.println("===========================================");
    }

    public void dynamicResize(int newCoreSize, int newMaxSize) {
        System.out.println("Resizing thread pool [" + poolName + "] from " + 
            getCorePoolSize() + "/" + getMaximumPoolSize() + " to " + 
            newCoreSize + "/" + newMaxSize);
        setCorePoolSize(newCoreSize);
        setMaximumPoolSize(newMaxSize);
    }

    public int getActiveThreads() {
        return activeThreads.get();
    }

    public long getCompletedTasks() {
        return completedTasks.get();
    }

    private static class BootCloudThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String poolName;

        public BootCloudThreadFactory(String poolName) {
            this.poolName = poolName;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, poolName + "-thread-" + threadNumber.getAndIncrement());
            thread.setDaemon(false);
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        }
    }

    private static class BootCloudRejectedExecutionHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            System.err.println("Task rejected from thread pool. Active: " + 
                executor.getActiveCount() + ", Pool size: " + executor.getPoolSize() + 
                ", Queue size: " + executor.getQueue().size());
            
            if (!executor.isShutdown()) {
                try {
                    executor.getQueue().put(r);
                    System.out.println("Task re-queued successfully");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RejectedExecutionException("Task re-queue interrupted", e);
                }
            }
        }
    }
}
