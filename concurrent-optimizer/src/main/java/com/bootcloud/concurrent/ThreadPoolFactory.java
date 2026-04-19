package com.bootcloud.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 统一的线程池工厂类
 * 
 * 提供传统线程池（CPU密集型/IO密集型）和虚拟线程池的统一创建入口
 * 消除代码重复，提升代码可维护性和可用性
 * 
 * @author BootCloud
 * @date 2026-04-19
 */
public class ThreadPoolFactory {

    /**
     * 私有构造方法，防止实例化
     */
    private ThreadPoolFactory() {
        throw new UnsupportedOperationException("工厂类不允许实例化");
    }

    /**
     * 创建CPU密集型传统线程池
     * 
     * 适用于：计算密集型任务（如加密、编译、数据处理等）
     * 配置规则：核心线程数=CPU核心数，最大线程数=CPU核心数+1
     * 
     * @param poolName 线程池名称，用于监控和日志
     * @return TraditionalThreadPool实例
     */
    public static TraditionalThreadPool createCpuIntensivePool(String poolName) {
        return TraditionalThreadPool.createCpuIntensivePool(poolName);
    }

    /**
     * 创建IO密集型传统线程池
     * 
     * 适用于：IO密集型任务（如数据库操作、文件读写、网络请求等）
     * 配置规则：核心线程数=CPU核心数*2，最大线程数=CPU核心数*4
     * 
     * @param poolName 线程池名称，用于监控和日志
     * @return TraditionalThreadPool实例
     */
    public static TraditionalThreadPool createIoIntensivePool(String poolName) {
        return TraditionalThreadPool.createIoIntensivePool(poolName);
    }

    /**
     * 创建虚拟线程池
     * 
     * 适用于：高并发场景，特别是IO密集型任务
     * 基于Java 21虚拟线程实现，支持大量虚拟线程的管理和调度
     * 
     * @param poolName 线程池名称，用于监控和日志
     * @return VirtualThreadPool实例
     */
    public static VirtualThreadPool createVirtualThreadPool(String poolName) {
        return VirtualThreadPool.create(poolName);
    }

    /**
     * 创建默认虚拟线程池
     * 
     * @return VirtualThreadPool实例
     */
    public static VirtualThreadPool createVirtualThreadPool() {
        return VirtualThreadPool.create();
    }

    /**
     * 根据任务类型创建对应的线程池
     * 
     * @param poolName 线程池名称
     * @param taskType 任务类型（CPU密集型、IO密集型、虚拟线程）
     * @return ExecutorService实例
     */
    public static ExecutorService createThreadPool(String poolName, TaskType taskType) {
        switch (taskType) {
            case CPU_INTENSIVE:
                return createCpuIntensivePool(poolName);
            case IO_INTENSIVE:
                return createIoIntensivePool(poolName);
            case VIRTUAL:
                return Executors.newVirtualThreadPerTaskExecutor();
            default:
                return createIoIntensivePool(poolName);
        }
    }

    /**
     * 任务类型枚举
     */
    public enum TaskType {
        /**
         * CPU密集型任务：计算、加密、编译等
         */
        CPU_INTENSIVE,
        
        /**
         * IO密集型任务：数据库操作、文件读写、网络请求等
         */
        IO_INTENSIVE,
        
        /**
         * 虚拟线程：高并发场景，特别是IO密集型任务
         */
        VIRTUAL
    }
}
