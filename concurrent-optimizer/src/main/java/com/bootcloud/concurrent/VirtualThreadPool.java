package com.bootcloud.concurrent;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 虚拟线程池（基于Java 21虚拟线程）
 * 
 * 功能：支持虚拟线程的创建、管理和调度，提供轻量级线程池实现
 * 适用于高并发场景，特别是IO密集型任务
 * 与TraditionalThreadPool形成对比，展示虚拟线程与传统线程的差异
 * 
 * @author BootCloud
 * @date 2026-04-19
 */
public class VirtualThreadPool {

    private final ExecutorService executor;
    private final String name;
    private final AtomicInteger taskCount = new AtomicInteger(0);
    private final AtomicInteger completedTaskCount = new AtomicInteger(0);
    private final long startTime;

    /**
     * 创建默认的虚拟线程池
     */
    public VirtualThreadPool() {
        this("virtual-thread-pool");
    }

    /**
     * 创建指定名称的虚拟线程池
     * 
     * @param name 线程池名称
     */
    public VirtualThreadPool(String name) {
        this.name = name;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.startTime = System.currentTimeMillis();
    }

    /**
     * 创建虚拟线程池
     * 
     * @param name 线程池名称
     * @return VirtualThreadPool实例
     */
    public static VirtualThreadPool create(String name) {
        return new VirtualThreadPool(name);
    }

    /**
     * 创建默认虚拟线程池
     * 
     * @return VirtualThreadPool实例
     */
    public static VirtualThreadPool create() {
        return new VirtualThreadPool();
    }

    /**
     * 创建虚拟线程池构建器
     * 
     * @return 构建器实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 创建一个虚拟线程
     * 
     * @param runnable 要执行的任务
     * @return 虚拟线程实例
     */
    public static Thread createVirtualThread(Runnable runnable) {
        return Thread.ofVirtual().start(runnable);
    }

    /**
     * 创建一个命名的虚拟线程
     * 
     * @param name 线程名称
     * @param runnable 要执行的任务
     * @return 虚拟线程实例
     */
    public static Thread createNamedVirtualThread(String name, Runnable runnable) {
        return Thread.ofVirtual().name(name).start(runnable);
    }

    /**
     * 创建虚拟线程池（返回ExecutorService）
     * 
     * @return 虚拟线程池实例
     */
    public static ExecutorService createVirtualThreadPool() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * 创建带有自定义线程工厂的虚拟线程池
     * 
     * @param threadFactory 线程工厂
     * @return 虚拟线程池实例
     */
    public static ExecutorService createVirtualThreadPool(ThreadFactory threadFactory) {
        return Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory());
    }

    /**
     * 优雅关闭线程池
     * 
     * @param executor 线程池
     * @param timeout 超时时间
     * @param unit 时间单位
     */
    public static void shutdown(ExecutorService executor, long timeout, TimeUnit unit) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(timeout, unit)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 提交任务到线程池
     * 
     * @param task 要执行的任务
     * @return Future对象，用于获取任务执行结果
     */
    public Future<?> submit(Runnable task) {
        taskCount.incrementAndGet();
        return executor.submit(() -> {
            try {
                task.run();
            } finally {
                completedTaskCount.incrementAndGet();
            }
        });
    }

    /**
     * 提交任务到线程池
     * 
     * @param task 要执行的任务
     * @param <T> 任务返回类型
     * @return Future对象，用于获取任务执行结果
     */
    public <T> Future<T> submit(Callable<T> task) {
        taskCount.incrementAndGet();
        return executor.submit(() -> {
            try {
                return task.call();
            } finally {
                completedTaskCount.incrementAndGet();
            }
        });
    }

    /**
     * 执行任务
     * 
     * @param task 要执行的任务
     */
    public void execute(Runnable task) {
        taskCount.incrementAndGet();
        executor.execute(() -> {
            try {
                task.run();
            } finally {
                completedTaskCount.incrementAndGet();
            }
        });
    }

    /**
     * 批量执行任务
     * 
     * @param tasks 任务集合
     */
    public void executeBatch(Iterable<Runnable> tasks) {
        tasks.forEach(this::execute);
    }

    /**
     * 关闭线程池
     */
    public void shutdown() {
        executor.shutdown();
    }

    /**
     * 立即关闭线程池
     * 
     * @return 未执行的任务列表
     */
    public java.util.List<Runnable> shutdownNow() {
        return executor.shutdownNow();
    }

    /**
     * 等待线程池关闭
     * 
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 是否在超时前关闭
     * @throws InterruptedException 中断异常
     */
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }

    /**
     * 检查线程池是否已关闭
     * 
     * @return 是否已关闭
     */
    public boolean isShutdown() {
        return executor.isShutdown();
    }

    /**
     * 检查线程池是否已终止
     * 
     * @return 是否已终止
     */
    public boolean isTerminated() {
        return executor.isTerminated();
    }

    /**
     * 打印线程池监控信息
     */
    public void printStatistics() {
        long uptime = System.currentTimeMillis() - startTime;
        double avgTasksPerSecond = completedTaskCount.get() * 1000.0 / uptime;
        
        System.out.println("=== 虚拟线程池监控信息 [" + name + "] ===");
        System.out.println("线程模型: 虚拟线程");
        System.out.println("已提交任务数: " + taskCount.get());
        System.out.println("已完成任务数: " + completedTaskCount.get());
        System.out.println("平均每秒处理任务数: " + String.format("%.2f", avgTasksPerSecond));
        System.out.println("线程池运行时长: " + (uptime / 1000.0) + "秒");
        System.out.println("===========================================");
    }

    /**
     * 获取线程池名称
     * 
     * @return 线程池名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取已提交的任务数
     * 
     * @return 已提交的任务数
     */
    public int getTaskCount() {
        return taskCount.get();
    }

    /**
     * 获取已完成的任务数
     * 
     * @return 已完成的任务数
     */
    public int getCompletedTaskCount() {
        return completedTaskCount.get();
    }

    /**
     * 虚拟线程池构建器
     */
    public static class Builder {
        private String name = "virtual-thread-pool";

        /**
         * 设置线程池名称
         * 
         * @param name 线程池名称
         * @return 构建器实例
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * 构建虚拟线程池
         * 
         * @return 虚拟线程池实例
         */
        public VirtualThreadPool build() {
            return new VirtualThreadPool(name);
        }
    }

    /**
     * 虚拟线程构建器
     * 提供链式调用创建虚拟线程
     */
    public static class VirtualThreadBuilder {
        private Thread.Builder.OfVirtual builder;

        private VirtualThreadBuilder() {
            this.builder = Thread.ofVirtual();
        }

        /**
         * 创建虚拟线程构建器
         * 
         * @return 构建器实例
         */
        public static VirtualThreadBuilder builder() {
            return new VirtualThreadBuilder();
        }

        /**
         * 设置线程名称
         * 
         * @param name 线程名称
         * @return 构建器实例
         */
        public VirtualThreadBuilder name(String name) {
            builder = builder.name(name);
            return this;
        }

        /**
         * 设置线程名称前缀和编号
         * 
         * @param prefix 名称前缀
         * @param start 起始编号
         * @return 构建器实例
         */
        public VirtualThreadBuilder name(String prefix, long start) {
            builder = builder.name(prefix, start);
            return this;
        }

        /**
         * 构建并启动虚拟线程
         * 
         * @param runnable 要执行的任务
         * @return 启动的虚拟线程
         */
        public Thread start(Runnable runnable) {
            return builder.start(runnable);
        }
    }
}
