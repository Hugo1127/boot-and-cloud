package com.bootcloud.concurrent;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 传统线程池（基于平台线程）
 * 
 * 功能：支持CPU密集型/IO密集型线程池自动创建、线程池监控、动态调整大小、自定义拒绝策略、任务异常捕获
 * 适用于传统平台线程场景，与虚拟线程池形成对比
 * 
 * @author BootCloud
 * @date 2026-04-19
 */
public class TraditionalThreadPool extends ThreadPoolExecutor {
    
    private final String poolName;
    private final AtomicLong completedTasks;
    private final AtomicInteger activeThreads;
    private final long startTime;

    /**
     * 构造方法：初始化线程池
     * 
     * @param corePoolSize 核心线程数
     * @param maximumPoolSize 最大线程数
     * @param keepAliveTime 非核心线程空闲超时时间
     * @param unit 时间单位
     * @param workQueue 任务阻塞队列
     * @param poolName 线程池名称
     */
    public TraditionalThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                                 TimeUnit unit, BlockingQueue<Runnable> workQueue, String poolName) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
              new BootCloudThreadFactory(poolName), new BootCloudRejectedExecutionHandler());
        this.poolName = poolName;
        this.completedTasks = new AtomicLong(0);
        this.activeThreads = new AtomicInteger(0);
        this.startTime = System.currentTimeMillis();
    }

    /**
     * 创建【CPU密集型】线程池
     * 
     * 适用于：计算、加密、编译等耗CPU任务
     * 配置规则：核心线程数=CPU核心数，最大线程数=CPU核心数+1，队列容量100
     * 
     * @param poolName 线程池名称
     * @return TraditionalThreadPool实例
     */
    public static TraditionalThreadPool createCpuIntensivePool(String poolName) {
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        int maxPoolSize = Runtime.getRuntime().availableProcessors() + 1;
        return new TraditionalThreadPool(
            corePoolSize,
            maxPoolSize,
            60L, TimeUnit.SECONDS,
            new MyBlockingQueue<>(100),
            poolName
        );
    }

    /**
     * 创建【IO密集型】线程池
     *
     * 适用于：接口调用、数据库操作、文件读写等IO等待任务
     * 配置规则：核心线程数=CPU核心数*2，最大线程数=CPU核心数*4，队列容量1000
     *
     * @param poolName 线程池名称
     * @return TraditionalThreadPool实例
     */
    public static TraditionalThreadPool createIoIntensivePool(String poolName) {
        int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;
        int maxPoolSize = Runtime.getRuntime().availableProcessors() * 4;
        return new TraditionalThreadPool(
            corePoolSize,
            maxPoolSize,
            60L, TimeUnit.SECONDS,
            new MyBlockingQueue<>(1000),
            poolName
        );
    }

    /**
     * 任务执行前调用
     * 
     * @param t 执行任务的线程
     * @param r 要执行的任务
     */
    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        activeThreads.incrementAndGet();
    }

    /**
     * 任务执行后调用
     * 
     * @param r 已执行的任务
     * @param t 任务执行过程中抛出的异常
     */
    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        activeThreads.decrementAndGet();
        completedTasks.incrementAndGet();
        
        if (t != null) {
            System.err.println("任务执行失败: " + t.getMessage());
            t.printStackTrace();
        }
    }

    /**
     * 打印线程池实时监控数据
     * 
     * 包含：线程数、队列、完成任务、吞吐量、运行时长等
     */
    public void printStatistics() {
        long uptime = System.currentTimeMillis() - startTime;
        double avgTasksPerSecond = completedTasks.get() * 1000.0 / uptime;
        
        System.out.println("=== 线程池监控信息 [" + poolName + "] ===");
        System.out.println("核心线程数: " + getCorePoolSize());
        System.out.println("最大线程数: " + getMaximumPoolSize());
        System.out.println("当前线程池线程总数: " + getPoolSize());
        System.out.println("活跃线程数: " + getActiveCount());
        System.out.println("等待队列大小: " + getQueue().size());
        System.out.println("已完成任务数: " + getCompletedTaskCount());
        System.out.println("总任务数: " + getTaskCount());
        System.out.println("平均每秒处理任务数: " + String.format("%.2f", avgTasksPerSecond));
        System.out.println("线程池运行时长: " + (uptime / 1000.0) + "秒");
        System.out.println("===========================================");
    }

    /**
     * 动态调整线程池大小
     * 
     * @param newCoreSize 新的核心线程数
     * @param newMaxSize 新的最大线程数
     */
    public void dynamicResize(int newCoreSize, int newMaxSize) {
        System.out.println("调整线程池 [" + poolName + "] 大小：从 " +
            getCorePoolSize() + "/" + getMaximumPoolSize() + " 调整为 " +
            newCoreSize + "/" + newMaxSize);
        setCorePoolSize(newCoreSize);
        setMaximumPoolSize(newMaxSize);
    }

    /**
     * 获取当前活跃线程数
     * 
     * @return 活跃线程数
     */
    public int getActiveThreads() {
        return activeThreads.get();
    }

    /**
     * 获取已完成任务总数
     * 
     * @return 已完成任务数
     */
    public long getCompletedTasks() {
        return completedTasks.get();
    }

    /**
     * 自定义线程工厂
     * 
     * 作用：给线程设置自定义名称、是否守护线程、优先级，方便日志排查
     */
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

    /**
     * 自定义拒绝策略
     * 
     * JDK默认策略：直接抛出异常
     * 自定义策略：打印日志，重新将任务放入队列等待执行
     */
    private static class BootCloudRejectedExecutionHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            System.err.println("任务被拒绝！活跃线程数: " +
                executor.getActiveCount() + ", 当前线程总数: " + executor.getPoolSize() +
                ", 等待队列大小: " + executor.getQueue().size());
            
            if (!executor.isShutdown()) {
                try {
                    executor.getQueue().put(r);
                    System.out.println("任务重新入队成功");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RejectedExecutionException("任务重新入队被中断", e);
                }
            }
        }
    }
}
