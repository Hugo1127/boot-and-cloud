package com.bootcloud.concurrent;

import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/**
 * SmartThreadPool 线程池工具类的单元测试类
 * 覆盖：CPU密集型/IO密集型线程池创建、任务执行、统计打印、动态扩容、拒绝策略等核心功能测试
 */
public class SmartThreadPoolTest {

    /**
     * 测试创建CPU密集型线程池
     * 验证：线程池实例不为null、核心线程数≥1，测试完成后关闭线程池
     */
    @Test
    public void testCreateCpuIntensivePool() {
        // 创建CPU密集型线程池
        SmartThreadPool pool = SmartThreadPool.createCpuIntensivePool("test-cpu-pool");
        // 断言线程池实例创建成功
        assertNotNull(pool);
        // 断言核心线程数至少为1（符合CPU密集型线程池配置规则）
        assertTrue(pool.getCorePoolSize() >= 1);
        // 测试完成，关闭线程池释放资源
        pool.shutdown();
    }

    /**
     * 测试创建IO密集型线程池
     * 验证：线程池实例不为null、核心线程数≥2，测试完成后关闭线程池
     */
    @Test
    public void testCreateIoIntensivePool() {
        // 创建IO密集型线程池
        SmartThreadPool pool = SmartThreadPool.createIoIntensivePool("test-io-pool");
        // 断言线程池实例创建成功
        assertNotNull(pool);
        // 断言核心线程数至少为2（符合IO密集型线程池配置规则）
        assertTrue(pool.getCorePoolSize() >= 2);
        // 测试完成，关闭线程池释放资源
        pool.shutdown();
    }

    /**
     * 测试线程池执行异步任务
     * 验证：线程池能够正常执行10个任务，且在指定时间内完成所有任务
     * @throws InterruptedException 线程等待中断异常
     */
    @Test
    public void testTaskExecution() throws InterruptedException {
        // 创建CPU密集型线程池用于测试任务执行
        SmartThreadPool pool = SmartThreadPool.createCpuIntensivePool("test-execution-pool");
        // 计数器：等待10个任务全部执行完成
        CountDownLatch latch = new CountDownLatch(10);

        // 向线程池提交10个异步任务
        for (int i = 0; i < 10; i++) {
            pool.execute(() -> {
                try {
                    // 模拟任务执行耗时100ms
                    Thread.sleep(100);
                    // 任务执行完成，计数器减1
                    latch.countDown();
                } catch (InterruptedException e) {
                    // 恢复中断状态
                    Thread.currentThread().interrupt();
                }
            });
        }

        // 断言：5秒内所有任务执行完成（计数器归零）
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        // 关闭线程池
        pool.shutdown();
    }

    /**
     * 测试线程池统计信息打印功能
     * 验证：调用统计方法不会抛出异常
     */
    @Test
    public void testPrintStatistics() {
        SmartThreadPool pool = SmartThreadPool.createCpuIntensivePool("test-stats-pool");
        // 断言：打印线程池统计信息不抛出任何异常
        assertDoesNotThrow(() -> pool.printStatistics());
        pool.shutdown();
    }

    /**
     * 测试线程池动态调整线程数功能
     * 验证：动态修改核心线程数、最大线程数后，参数与预期一致
     */
    @Test
    public void testDynamicResize() {
        SmartThreadPool pool = SmartThreadPool.createCpuIntensivePool("test-resize-pool");
        // 获取线程池原始配置
        int originalCoreSize = pool.getCorePoolSize();
        int originalMaxSize = pool.getMaximumPoolSize();
        
        // 计算新的线程数（保证参数合法）
        int newCoreSize = Math.min(originalCoreSize + 2, originalMaxSize);
        int newMaxSize = Math.max(originalCoreSize + 4, originalMaxSize);
        
        // 执行动态扩容
        pool.dynamicResize(newCoreSize, newMaxSize);
        // 断言核心线程数修改成功
        assertEquals(newCoreSize, pool.getCorePoolSize());
        // 断言最大线程数修改成功
        assertEquals(newMaxSize, pool.getMaximumPoolSize());
        
        pool.shutdown();
    }

    /**
     * 测试线程池拒绝策略/饱和处理机制
     * 构造：核心线程=1，最大线程=1，队列容量=1 → 最多同时处理2个任务，提交3个任务触发拒绝/排队逻辑
     * 验证：所有任务最终都能被正常执行完成
     * @throws InterruptedException 线程等待中断异常
     */
    @Test
    public void testRejectionHandler() throws InterruptedException {
        // 手动创建极小容量线程池，用于触发拒绝策略
        SmartThreadPool pool = new SmartThreadPool(1, 1, 60, TimeUnit.SECONDS, 
            new java.util.concurrent.LinkedBlockingQueue<>(1), "test-rejection-pool");
        // 计数器：等待3个任务执行完成
        CountDownLatch latch = new CountDownLatch(3);

        // 提交3个任务，超过线程池最大处理能力
        for (int i = 0; i < 3; i++) {
            final int taskId = i;
            pool.execute(() -> {
                try {
                    // 模拟长耗时任务
                    Thread.sleep(1000);
                    latch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // 断言5秒内所有任务执行完成（验证拒绝策略正常工作，未丢失任务）
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        pool.shutdown();
    }
}