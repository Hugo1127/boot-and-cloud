package com.bootcloud.concurrent;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 智能线程池
 * 功能：支持CPU密集型/IO密集型线程池自动创建、线程池监控、动态调整大小、自定义拒绝策略、任务异常捕获
 */
public class SmartThreadPool extends ThreadPoolExecutor {
    // 线程池名称，用于区分不同业务线程池
    private final String poolName;
    // 原子类：统计已完成任务总数（线程安全）
    private final AtomicLong completedTasks;
    // 原子类：统计当前活跃线程数（线程安全）
    private final AtomicInteger activeThreads;
    // 线程池启动时间戳
    private final long startTime;

    /**
     * 构造方法：初始化线程池
     * @param corePoolSize 核心线程数
     * @param maximumPoolSize 最大线程数
     * @param keepAliveTime 非核心线程空闲超时时间
     * @param unit 时间单位
     * @param workQueue 任务阻塞队列
     * @param poolName 线程池名称
     */
    public SmartThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                         TimeUnit unit, BlockingQueue<Runnable> workQueue, String poolName) {
        // 调用父类构造器，传入自定义线程工厂+拒绝策略
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
              new BootCloudThreadFactory(poolName), new BootCloudRejectedExecutionHandler());
        this.poolName = poolName;
        // 初始化原子计数器
        this.completedTasks = new AtomicLong(0);
        this.activeThreads = new AtomicInteger(0);
        // 记录线程池启动时间
        this.startTime = System.currentTimeMillis();
    }

    /**
     * 创建【CPU密集型】线程池（适用于计算、加密、编译等耗CPU任务）
     * 配置规则：核心线程数=CPU核心数，最大线程数=CPU核心数+1，队列容量100
     * CPU密集型任务，建议创建与CPU核心数相同的线程数，以充分利用CPU资源
     */
    public static SmartThreadPool createCpuIntensivePool(String poolName) {
        // 获取服务器CPU核心数
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        int maxPoolSize = Runtime.getRuntime().availableProcessors() + 1;
        return new SmartThreadPool(
            corePoolSize,
            maxPoolSize,
            60L, TimeUnit.SECONDS,  // 空闲线程60秒回收
            new LinkedBlockingQueue<>(100),  // 有界队列，容量100
            poolName
        );
    }

    /**
     * 创建【IO密集型】线程池（适用于接口调用、数据库操作、文件读写等IO等待任务）
     * 配置规则：核心线程数=CPU核心数*2，最大线程数=CPU核心数*4，队列容量1000
     * IO密集型可以创建更多线程，以提高吞吐量，同时保持队列容量足够大，以避免任务积压
     */
    public static SmartThreadPool createIoIntensivePool(String poolName) {
        int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;
        int maxPoolSize = Runtime.getRuntime().availableProcessors() * 4;
        return new SmartThreadPool(
            corePoolSize,
            maxPoolSize,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),  // IO任务多，队列容量更大
            poolName
        );
    }

    /**
     * 任务执行【前】调用
     * 作用：活跃线程数+1
     */
    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        activeThreads.incrementAndGet();
    }

    /**
     * 任务执行【后】调用
     * 作用：活跃线程数-1，完成任务数+1，捕获任务执行异常
     */
    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        activeThreads.decrementAndGet();
        completedTasks.incrementAndGet();
        
        // 如果任务执行抛出异常，打印错误日志
        if (t != null) {
            System.err.println("任务执行失败: " + t.getMessage());
            t.printStackTrace();
        }
    }

    /**
     * 打印线程池实时监控数据（核心功能：监控）
     * 包含：线程数、队列、完成任务、吞吐量、运行时长等
     */
    public void printStatistics() {
        long uptime = System.currentTimeMillis() - startTime;
        // 计算平均每秒处理任务数
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
     * 动态调整线程池大小（核心功能：动态扩容/缩容）
     * 无需重启服务，直接修改核心线程数和最大线程数
     */
    public void dynamicResize(int newCoreSize, int newMaxSize) {
        System.out.println("调整线程池 [" + poolName + "] 大小：从 " +
            getCorePoolSize() + "/" + getMaximumPoolSize() + " 调整为 " +
            newCoreSize + "/" + newMaxSize);
        setCorePoolSize(newCoreSize);
        setMaximumPoolSize(newMaxSize);
    }

    // 获取当前活跃线程数
    public int getActiveThreads() {
        return activeThreads.get();
    }

    // 获取已完成任务总数
    public long getCompletedTasks() {
        return completedTasks.get();
    }

    /**
     * 自定义线程工厂
     * 作用：给线程设置自定义名称、是否守护线程、优先级，方便日志排查
     */
    private static class BootCloudThreadFactory implements ThreadFactory {
        // 线程编号，自增
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String poolName;

        public BootCloudThreadFactory(String poolName) {
            this.poolName = poolName;
        }

        @Override
        public Thread newThread(Runnable r) {
            // 创建线程，设置自定义名称（关键：方便日志定位问题）
            Thread thread = new Thread(r, poolName + "-thread-" + threadNumber.getAndIncrement());
            // 设置为非守护线程（业务线程必须执行完成，不能随主线程退出）
            thread.setDaemon(false);
            // 默认优先级
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        }
    }

    /**
     *CallerRunsPolicy，使用线程池的调用者所在的线程去执行被拒绝的任务，除非线程池被停止或者线程池的任务队列已有空缺。
     *AbortPolicy，直接抛出一个任务被线程池拒绝的异常。
     *DiscardPolicy，不做任何处理，静默拒绝提交的任务。
     *DiscardOldestPolicy，抛弃最老的任务，然后执行该任务
     * 自定义拒绝策略
     * JDK默认：直接抛出异常
     * 自定义：打印日志，重新将任务放入队列等待执行
     */
    private static class BootCloudRejectedExecutionHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            System.err.println("任务被拒绝！活跃线程数: " +
                executor.getActiveCount() + ", 当前线程总数: " + executor.getPoolSize() +
                ", 等待队列大小: " + executor.getQueue().size());
            
            // 如果线程池未关闭，尝试将任务重新放入队列
            if (!executor.isShutdown()) {
                try {
                    // put()方法：队列满了会阻塞，直到有空间
                    executor.getQueue().put(r);
                    System.out.println("任务重新入队成功");
                } catch (InterruptedException e) {
                    // 恢复中断状态
                    Thread.currentThread().interrupt();
                    throw new RejectedExecutionException("任务重新入队被中断", e);
                }
            }
        }
    }
}