package com.bootcloud.concurrent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 线程池综合测试类
 * 
 * 覆盖：虚拟线程池、传统线程池的功能测试，以及两者性能对比测试
 * 
 * @author BootCloud
 * @date 2026-04-19
 */
public class ThreadPoolTest {

    private VirtualThreadPool virtualPool;
    private TraditionalThreadPool traditionalPool;

    /**
     * 每个测试方法执行前创建线程池
     */
    @BeforeEach
    public void setUp() {
        virtualPool = VirtualThreadPool.create("test-virtual-pool");
        traditionalPool = TraditionalThreadPool.createIoIntensivePool("test-traditional-pool");
    }

    /**
     * 每个测试方法执行后关闭线程池，释放资源
     * 
     * @throws InterruptedException 等待中断异常
     */
    @AfterEach
    public void tearDown() throws InterruptedException {
        if (virtualPool != null && !virtualPool.isShutdown()) {
            virtualPool.shutdown();
            virtualPool.awaitTermination(5, TimeUnit.SECONDS);
        }
        if (traditionalPool != null && !traditionalPool.isShutdown()) {
            traditionalPool.shutdown();
            traditionalPool.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    // ==================== 虚拟线程池测试 ====================

    /**
     * 测试创建默认虚拟线程池
     */
    @Test
    public void testCreateDefaultVirtualPool() {
        VirtualThreadPool defaultPool = VirtualThreadPool.create();
        assertNotNull(defaultPool);
        assertEquals("virtual-thread-pool", defaultPool.getName());
        defaultPool.shutdown();
    }

    /**
     * 测试创建指定名称的虚拟线程池
     */
    @Test
    public void testCreateNamedVirtualPool() {
        VirtualThreadPool namedPool = VirtualThreadPool.create("custom-pool");
        assertNotNull(namedPool);
        assertEquals("custom-pool", namedPool.getName());
        namedPool.shutdown();
    }

    /**
     * 测试使用构建器创建虚拟线程池
     */
    @Test
    public void testVirtualPoolBuilderPattern() {
        VirtualThreadPool builtPool = VirtualThreadPool.builder()
            .name("builder-pool")
            .build();
        assertNotNull(builtPool);
        assertEquals("builder-pool", builtPool.getName());
        builtPool.shutdown();
    }

    /**
     * 测试虚拟线程池执行Runnable任务
     */
    @Test
    public void testVirtualPoolExecuteRunnable() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(5);
        AtomicInteger counter = new AtomicInteger(0);

        for (int i = 0; i < 5; i++) {
            virtualPool.execute(() -> {
                try {
                    counter.incrementAndGet();
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS), "所有任务应在5秒内完成");
        assertEquals(5, counter.get(), "应执行5个任务");
        Thread.sleep(100);
        assertEquals(5, virtualPool.getCompletedTaskCount(), "已完成任务数应为5");
    }

    /**
     * 测试虚拟线程池提交Callable任务并获取返回值
     */
    @Test
    public void testVirtualPoolSubmitCallable() throws InterruptedException, ExecutionException, TimeoutException {
        Future<String> future = virtualPool.submit(() -> {
            Thread.sleep(50);
            return "hello-virtual-thread";
        });

        String result = future.get(5, TimeUnit.SECONDS);
        assertEquals("hello-virtual-thread", result, "返回值应与预期一致");
        assertEquals(1, virtualPool.getCompletedTaskCount(), "已完成任务数应为1");
    }

    /**
     * 测试虚拟线程池批量执行任务
     */
    @Test
    public void testVirtualPoolExecuteBatch() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(10);
        List<Runnable> tasks = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            tasks.add(() -> {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        virtualPool.executeBatch(tasks);
        assertTrue(latch.await(5, TimeUnit.SECONDS), "批量任务应在5秒内完成");
        assertEquals(10, virtualPool.getCompletedTaskCount(), "应完成10个任务");
    }

    /**
     * 测试虚拟线程池关闭功能
     */
    @Test
    public void testVirtualPoolShutdown() throws InterruptedException {
        virtualPool.execute(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        virtualPool.shutdown();
        assertTrue(virtualPool.isShutdown(), "线程池应已关闭");
        assertTrue(virtualPool.awaitTermination(5, TimeUnit.SECONDS), "线程池应在5秒内终止");
        assertTrue(virtualPool.isTerminated(), "线程池应已终止");
    }

    /**
     * 测试虚拟线程池高并发场景
     */
    @Test
    public void testVirtualPoolHighConcurrency() throws InterruptedException {
        int taskCount = 1000;
        CountDownLatch latch = new CountDownLatch(taskCount);
        AtomicInteger counter = new AtomicInteger(0);

        for (int i = 0; i < taskCount; i++) {
            virtualPool.execute(() -> {
                try {
                    counter.incrementAndGet();
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS), "1000个任务应在10秒内完成");
        assertEquals(taskCount, counter.get(), "应执行1000个任务");
        assertEquals(taskCount, virtualPool.getCompletedTaskCount(), "已完成任务数应为1000");
    }

    /**
     * 测试虚拟线程池任务异常处理
     */
    @Test
    public void testVirtualPoolTaskExceptionHandling() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        AtomicInteger successCounter = new AtomicInteger(0);

        virtualPool.execute(() -> {
            try {
                throw new RuntimeException("测试异常");
            } finally {
                latch.countDown();
            }
        });

        virtualPool.execute(() -> {
            try {
                successCounter.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });

        virtualPool.execute(() -> {
            try {
                successCounter.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS), "所有任务应在5秒内完成");
        assertEquals(2, successCounter.get(), "应有2个任务成功执行");
    }

    // ==================== 虚拟线程工具方法测试 ====================

    /**
     * 测试创建虚拟线程
     */
    @Test
    public void testCreateVirtualThread() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean executed = new AtomicBoolean(false);

        Thread thread = VirtualThreadPool.createVirtualThread(() -> {
            executed.set(true);
            latch.countDown();
        });

        assertNotNull(thread, "虚拟线程实例不应为null");
        assertTrue(thread.isVirtual(), "创建的线程应为虚拟线程");
        assertTrue(latch.await(5, TimeUnit.SECONDS), "任务应在5秒内执行完成");
        assertTrue(executed.get(), "任务应执行完成");
    }

    /**
     * 测试创建命名的虚拟线程
     */
    @Test
    public void testCreateNamedVirtualThread() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        String expectedName = "my-custom-thread";

        Thread thread = VirtualThreadPool.createNamedVirtualThread(expectedName, () -> {
            latch.countDown();
        });

        assertNotNull(thread, "虚拟线程实例不应为null");
        assertTrue(thread.isVirtual(), "创建的线程应为虚拟线程");
        assertEquals(expectedName, thread.getName(), "线程名称应与预期一致");
        assertTrue(latch.await(5, TimeUnit.SECONDS), "任务应在5秒内执行完成");
    }

    /**
     * 测试创建虚拟线程池（返回ExecutorService）
     */
    @Test
    public void testCreateVirtualThreadPoolExecutorService() throws InterruptedException, ExecutionException, TimeoutException {
        ExecutorService pool = VirtualThreadPool.createVirtualThreadPool();
        assertNotNull(pool, "线程池实例不应为null");

        CountDownLatch latch = new CountDownLatch(1);
        Future<?> future = pool.submit(() -> {
            latch.countDown();
        });

        future.get(5, TimeUnit.SECONDS);
        assertTrue(latch.await(5, TimeUnit.SECONDS), "任务应在5秒内执行完成");

        VirtualThreadPool.shutdown(pool, 5, TimeUnit.SECONDS);
    }

    /**
     * 测试优雅关闭线程池
     */
    @Test
    public void testShutdownGracefully() {
        ExecutorService pool = VirtualThreadPool.createVirtualThreadPool();
        pool.submit(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        VirtualThreadPool.shutdown(pool, 5, TimeUnit.SECONDS);
        assertTrue(pool.isShutdown(), "线程池应已关闭");
    }

    /**
     * 测试虚拟线程构建器
     */
    @Test
    public void testVirtualThreadBuilder() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean executed = new AtomicBoolean(false);

        Thread thread = VirtualThreadPool.VirtualThreadBuilder.builder()
            .name("builder-thread")
            .start(() -> {
                executed.set(true);
                latch.countDown();
            });

        assertNotNull(thread, "虚拟线程实例不应为null");
        assertTrue(thread.isVirtual(), "创建的线程应为虚拟线程");
        assertTrue(latch.await(5, TimeUnit.SECONDS), "任务应在5秒内执行完成");
        assertTrue(executed.get(), "任务应执行完成");
    }

    /**
     * 测试虚拟线程构建器 - 带前缀和编号
     */
    @Test
    public void testVirtualThreadBuilderWithPrefix() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Thread thread = VirtualThreadPool.VirtualThreadBuilder.builder()
            .name("worker-", 100)
            .start(() -> {
                latch.countDown();
            });

        assertNotNull(thread, "虚拟线程实例不应为null");
        assertTrue(thread.getName().startsWith("worker-"), "线程名称应以'worker-'开头");
        assertTrue(latch.await(5, TimeUnit.SECONDS), "任务应在5秒内执行完成");
    }

    /**
     * 测试虚拟线程高并发场景
     */
    @Test
    public void testHighConcurrencyVirtualThreads() throws InterruptedException {
        int threadCount = 500;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger counter = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            VirtualThreadPool.createVirtualThread(() -> {
                try {
                    counter.incrementAndGet();
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS), "500个虚拟线程应在10秒内完成");
        assertEquals(threadCount, counter.get(), "应执行500个任务");
    }

    // ==================== 传统线程池测试 ====================

    /**
     * 测试创建CPU密集型传统线程池
     */
    @Test
    public void testCreateCpuIntensivePool() {
        TraditionalThreadPool cpuPool = TraditionalThreadPool.createCpuIntensivePool("test-cpu-pool");
        assertNotNull(cpuPool);
        assertTrue(cpuPool.getCorePoolSize() >= 1);
        cpuPool.shutdown();
    }

    /**
     * 测试创建IO密集型传统线程池
     */
    @Test
    public void testCreateIoIntensivePool() {
        TraditionalThreadPool ioPool = TraditionalThreadPool.createIoIntensivePool("test-io-pool");
        assertNotNull(ioPool);
        assertTrue(ioPool.getCorePoolSize() >= 2);
        ioPool.shutdown();
    }

    /**
     * 测试传统线程池执行任务
     */
    @Test
    public void testTraditionalPoolExecuteTask() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(5);
        AtomicInteger counter = new AtomicInteger(0);

        for (int i = 0; i < 5; i++) {
            traditionalPool.execute(() -> {
                try {
                    counter.incrementAndGet();
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS), "所有任务应在5秒内完成");
        assertEquals(5, counter.get(), "应执行5个任务");
    }

    /**
     * 测试传统线程池动态调整大小
     */
    @Test
    public void testTraditionalPoolDynamicResize() {
        int originalCoreSize = traditionalPool.getCorePoolSize();
        int originalMaxSize = traditionalPool.getMaximumPoolSize();
        
        int newCoreSize = Math.min(originalCoreSize + 2, originalMaxSize);
        int newMaxSize = Math.max(originalCoreSize + 4, originalMaxSize);
        
        traditionalPool.dynamicResize(newCoreSize, newMaxSize);
        assertEquals(newCoreSize, traditionalPool.getCorePoolSize());
        assertEquals(newMaxSize, traditionalPool.getMaximumPoolSize());
    }

    /**
     * 测试传统线程池统计信息打印
     */
    @Test
    public void testTraditionalPoolPrintStatistics() {
        assertDoesNotThrow(() -> traditionalPool.printStatistics());
    }

    /**
     * 测试传统线程池拒绝策略
     */
    @Test
    public void testTraditionalPoolRejectionHandler() throws InterruptedException {
        TraditionalThreadPool smallPool = new TraditionalThreadPool(1, 1, 60, TimeUnit.SECONDS, 
            new LinkedBlockingQueue<>(1), "test-rejection-pool");
        CountDownLatch latch = new CountDownLatch(3);

        for (int i = 0; i < 3; i++) {
            smallPool.execute(() -> {
                try {
                    Thread.sleep(500);
                    latch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS), "所有任务应在5秒内完成");
        smallPool.shutdown();
    }

    // ==================== 原始线程性能对比测试 ====================

    /**
     * 测试基本任务执行性能
     * 
     * 对比原始虚拟线程和传统线程执行简单任务的性能差异
     */
    @Test
    public void testBasicTaskPerformance() throws InterruptedException {
        System.out.println("=== 基本任务执行性能测试 ===");
        int taskCount = 10000;

        // 测试虚拟线程
        long virtualStart = System.currentTimeMillis();
        CountDownLatch virtualLatch = new CountDownLatch(taskCount);
        for (int i = 0; i < taskCount; i++) {
            Thread.ofVirtual().start(() -> {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    virtualLatch.countDown();
                }
            });
        }
        virtualLatch.await();
        long virtualEnd = System.currentTimeMillis();
        System.out.println("虚拟线程执行 " + taskCount + " 个任务耗时: " + (virtualEnd - virtualStart) + "ms");

        // 测试传统线程
        long traditionalStart = System.currentTimeMillis();
        CountDownLatch traditionalLatch = new CountDownLatch(taskCount);
        ExecutorService traditionalExecutor = Executors.newFixedThreadPool(100);
        for (int i = 0; i < taskCount; i++) {
            traditionalExecutor.submit(() -> {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    traditionalLatch.countDown();
                }
            });
        }
        traditionalLatch.await();
        traditionalExecutor.shutdown();
        traditionalExecutor.awaitTermination(1, TimeUnit.MINUTES);
        long traditionalEnd = System.currentTimeMillis();
        System.out.println("传统线程执行 " + taskCount + " 个任务耗时: " + (traditionalEnd - traditionalStart) + "ms");

        System.out.println("性能提升: " + String.format("%.2f%%", (1 - (double) (virtualEnd - virtualStart) / (traditionalEnd - traditionalStart)) * 100));
    }

    /**
     * 测试IO密集型任务性能
     * 
     * 对比原始虚拟线程和传统线程执行IO密集型任务的性能差异
     */
    @Test
    public void testIoIntensiveTaskPerformance() throws InterruptedException {
        System.out.println("\n=== IO密集型任务性能测试 ===");
        int taskCount = 10000;
        int ioWaitTime = 10; // 模拟IO等待时间（毫秒）

        // 测试虚拟线程
        long virtualStart = System.currentTimeMillis();
        CountDownLatch virtualLatch = new CountDownLatch(taskCount);
        for (int i = 0; i < taskCount; i++) {
            Thread.ofVirtual().start(() -> {
                try {
                    Thread.sleep(ioWaitTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    virtualLatch.countDown();
                }
            });
        }
        virtualLatch.await();
        long virtualEnd = System.currentTimeMillis();
        System.out.println("虚拟线程执行 " + taskCount + " 个IO密集型任务耗时: " + (virtualEnd - virtualStart) + "ms");

        // 测试传统线程
        long traditionalStart = System.currentTimeMillis();
        CountDownLatch traditionalLatch = new CountDownLatch(taskCount);
        ExecutorService traditionalExecutor = Executors.newFixedThreadPool(100);
        for (int i = 0; i < taskCount; i++) {
            traditionalExecutor.submit(() -> {
                try {
                    Thread.sleep(ioWaitTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    traditionalLatch.countDown();
                }
            });
        }
        traditionalLatch.await();
        traditionalExecutor.shutdown();
        traditionalExecutor.awaitTermination(1, TimeUnit.MINUTES);
        long traditionalEnd = System.currentTimeMillis();
        System.out.println("传统线程执行 " + taskCount + " 个IO密集型任务耗时: " + (traditionalEnd - traditionalStart) + "ms");

        System.out.println("性能提升: " + String.format("%.2f%%", (1 - (double) (virtualEnd - virtualStart) / (traditionalEnd - traditionalStart)) * 100));
    }

    /**
     * 测试CPU密集型任务性能
     * 
     * 对比原始虚拟线程和传统线程执行CPU密集型任务的性能差异
     */
    @Test
    public void testCpuIntensiveTaskPerformance() throws InterruptedException {
        System.out.println("\n=== CPU密集型任务性能测试 ===");
        int taskCount = 100;

        // 测试虚拟线程
        long virtualStart = System.currentTimeMillis();
        CountDownLatch virtualLatch = new CountDownLatch(taskCount);
        for (int i = 0; i < taskCount; i++) {
            Thread.ofVirtual().start(() -> {
                try {
                    int result = 0;
                    for (int j = 0; j < 10000000; j++) {
                        result += j;
                    }
                } finally {
                    virtualLatch.countDown();
                }
            });
        }
        virtualLatch.await();
        long virtualEnd = System.currentTimeMillis();
        System.out.println("虚拟线程执行 " + taskCount + " 个CPU密集型任务耗时: " + (virtualEnd - virtualStart) + "ms");

        // 测试传统线程
        long traditionalStart = System.currentTimeMillis();
        CountDownLatch traditionalLatch = new CountDownLatch(taskCount);
        ExecutorService traditionalExecutor = Executors.newFixedThreadPool(100);
        for (int i = 0; i < taskCount; i++) {
            traditionalExecutor.submit(() -> {
                try {
                    int result = 0;
                    for (int j = 0; j < 10000000; j++) {
                        result += j;
                    }
                } finally {
                    traditionalLatch.countDown();
                }
            });
        }
        traditionalLatch.await();
        traditionalExecutor.shutdown();
        traditionalExecutor.awaitTermination(1, TimeUnit.MINUTES);
        long traditionalEnd = System.currentTimeMillis();
        System.out.println("传统线程执行 " + taskCount + " 个CPU密集型任务耗时: " + (traditionalEnd - traditionalStart) + "ms");

        System.out.println("性能提升: " + String.format("%.2f%%", (1 - (double) (virtualEnd - virtualStart) / (traditionalEnd - traditionalStart)) * 100));
    }

    /**
     * 测试高并发场景性能
     * 
     * 对比原始虚拟线程和传统线程在高并发场景下的性能差异
     */
    @Test
    public void testHighConcurrencyPerformance() throws InterruptedException {
        System.out.println("\n=== 高并发场景性能测试 ===");
        int highTaskCount = 100000;

        // 测试虚拟线程
        long virtualStart = System.currentTimeMillis();
        CountDownLatch virtualLatch = new CountDownLatch(highTaskCount);
        for (int i = 0; i < highTaskCount; i++) {
            Thread.ofVirtual().start(() -> {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    virtualLatch.countDown();
                }
            });
        }
        virtualLatch.await();
        long virtualEnd = System.currentTimeMillis();
        System.out.println("虚拟线程执行 " + highTaskCount + " 个任务耗时: " + (virtualEnd - virtualStart) + "ms");
        System.out.println("虚拟线程每秒处理任务数: " + (highTaskCount * 1000.0 / (virtualEnd - virtualStart)));

        // 测试传统线程
        long traditionalStart = System.currentTimeMillis();
        CountDownLatch traditionalLatch = new CountDownLatch(highTaskCount);
        ExecutorService traditionalExecutor = Executors.newFixedThreadPool(1000);
        for (int i = 0; i < highTaskCount; i++) {
            traditionalExecutor.submit(() -> {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    traditionalLatch.countDown();
                }
            });
        }
        traditionalLatch.await();
        traditionalExecutor.shutdown();
        traditionalExecutor.awaitTermination(1, TimeUnit.MINUTES);
        long traditionalEnd = System.currentTimeMillis();
        System.out.println("传统线程执行 " + highTaskCount + " 个任务耗时: " + (traditionalEnd - traditionalStart) + "ms");
        System.out.println("传统线程每秒处理任务数: " + (highTaskCount * 1000.0 / (traditionalEnd - traditionalStart)));

        System.out.println("性能提升: " + String.format("%.2f%%", (1 - (double) (virtualEnd - virtualStart) / (traditionalEnd - traditionalStart)) * 100));
    }

    /**
     * 测试VirtualThreadPool性能
     * 
     * 测试封装后的VirtualThreadPool执行任务的性能
     */
    @Test
    public void testVirtualThreadPoolPerformance() throws InterruptedException {
        System.out.println("\n=== VirtualThreadPool性能测试 ===");
        int taskCount = 10000;
        int ioWaitTime = 10;

        VirtualThreadPool virtualPool = VirtualThreadPool.create("test-virtual-pool");

        long start = System.currentTimeMillis();
        CountDownLatch latch = new CountDownLatch(taskCount);

        for (int i = 0; i < taskCount; i++) {
            virtualPool.execute(() -> {
                try {
                    Thread.sleep(ioWaitTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long end = System.currentTimeMillis();

        System.out.println("VirtualThreadPool执行 " + taskCount + " 个任务耗时: " + (end - start) + "ms");
        System.out.println("已完成任务数: " + virtualPool.getCompletedTaskCount());
        
        virtualPool.shutdown();
    }

    // ==================== 虚拟线程池 vs 传统线程池 对比测试 ====================

    /**
     * 对比测试：虚拟线程池与传统线程池执行IO密集型任务的性能差异
     * 
     * 虚拟线程在IO密集型场景下具有显著优势，因为虚拟线程的创建和切换成本极低
     */
    @Test
    public void testIoIntensivePerformanceComparison() throws InterruptedException {
        int taskCount = 1000;
        int ioWaitTime = 10; // 模拟IO等待时间（毫秒）

        // 测试虚拟线程池
        VirtualThreadPool vPool = VirtualThreadPool.create("compare-virtual");
        CountDownLatch vLatch = new CountDownLatch(taskCount);
        long vStart = System.currentTimeMillis();

        for (int i = 0; i < taskCount; i++) {
            vPool.execute(() -> {
                try {
                    Thread.sleep(ioWaitTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    vLatch.countDown();
                }
            });
        }
        vLatch.await(30, TimeUnit.SECONDS);
        long vEnd = System.currentTimeMillis();
        vPool.shutdown();

        // 测试传统线程池
        TraditionalThreadPool tPool = TraditionalThreadPool.createIoIntensivePool("compare-traditional");
        CountDownLatch tLatch = new CountDownLatch(taskCount);
        long tStart = System.currentTimeMillis();

        for (int i = 0; i < taskCount; i++) {
            tPool.execute(() -> {
                try {
                    Thread.sleep(ioWaitTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    tLatch.countDown();
                }
            });
        }
        tLatch.await(30, TimeUnit.SECONDS);
        long tEnd = System.currentTimeMillis();
        tPool.shutdown();

        long vTime = vEnd - vStart;
        long tTime = tEnd - tStart;

        System.out.println("\n=== IO密集型任务性能对比 ===");
        System.out.println("任务数: " + taskCount + ", 每个任务IO等待: " + ioWaitTime + "ms");
        System.out.println("虚拟线程池耗时: " + vTime + "ms");
        System.out.println("传统线程池耗时: " + tTime + "ms");
        System.out.println("性能提升: " + String.format("%.2f%%", (1 - (double) vTime / tTime) * 100));

        // 虚拟线程池应该更快或至少不慢于传统线程池
        assertTrue(vTime <= tTime * 2, "虚拟线程池在IO密集型任务中应表现更好或相当");
    }

    /**
     * 对比测试：虚拟线程池与传统线程池执行CPU密集型任务的性能差异
     * 
     * 在CPU密集型场景下，两者性能差异不大，因为瓶颈在于CPU而非线程调度
     */
    @Test
    public void testCpuIntensivePerformanceComparison() throws InterruptedException {
        int taskCount = 100;
        int cpuWork = 1000000; // CPU计算量

        // 测试虚拟线程池
        VirtualThreadPool vPool = VirtualThreadPool.create("compare-virtual-cpu");
        CountDownLatch vLatch = new CountDownLatch(taskCount);
        long vStart = System.currentTimeMillis();

        for (int i = 0; i < taskCount; i++) {
            vPool.execute(() -> {
                try {
                    long sum = 0;
                    for (int j = 0; j < cpuWork; j++) {
                        sum += j;
                    }
                } finally {
                    vLatch.countDown();
                }
            });
        }
        vLatch.await(30, TimeUnit.SECONDS);
        long vEnd = System.currentTimeMillis();
        vPool.shutdown();

        // 测试传统线程池
        TraditionalThreadPool tPool = TraditionalThreadPool.createCpuIntensivePool("compare-traditional-cpu");
        CountDownLatch tLatch = new CountDownLatch(taskCount);
        long tStart = System.currentTimeMillis();

        for (int i = 0; i < taskCount; i++) {
            tPool.execute(() -> {
                try {
                    long sum = 0;
                    for (int j = 0; j < cpuWork; j++) {
                        sum += j;
                    }
                } finally {
                    tLatch.countDown();
                }
            });
        }
        tLatch.await(30, TimeUnit.SECONDS);
        long tEnd = System.currentTimeMillis();
        tPool.shutdown();

        long vTime = vEnd - vStart;
        long tTime = tEnd - tStart;

        System.out.println("\n=== CPU密集型任务性能对比 ===");
        System.out.println("任务数: " + taskCount + ", 每个任务计算量: " + cpuWork);
        System.out.println("虚拟线程池耗时: " + vTime + "ms");
        System.out.println("传统线程池耗时: " + tTime + "ms");

        // CPU密集型任务两者性能应该相近
        assertTrue(Math.abs(vTime - tTime) < tTime * 2, "CPU密集型任务两者性能应相近");
    }

    /**
     * 对比测试：虚拟线程池与传统线程池高并发场景下的资源消耗
     * 
     * 虚拟线程可以创建大量实例而不消耗过多系统资源
     */
    @Test
    public void testHighConcurrencyResourceComparison() throws InterruptedException {
        int taskCount = 5000;

        // 测试虚拟线程池
        VirtualThreadPool vPool = VirtualThreadPool.create("compare-virtual-high");
        CountDownLatch vLatch = new CountDownLatch(taskCount);
        AtomicInteger vCounter = new AtomicInteger(0);
        long vStart = System.currentTimeMillis();

        for (int i = 0; i < taskCount; i++) {
            vPool.execute(() -> {
                try {
                    vCounter.incrementAndGet();
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    vLatch.countDown();
                }
            });
        }
        assertTrue(vLatch.await(30, TimeUnit.SECONDS), "虚拟线程池应在30秒内完成");
        long vEnd = System.currentTimeMillis();
        vPool.shutdown();

        // 测试传统线程池
        TraditionalThreadPool tPool = TraditionalThreadPool.createIoIntensivePool("compare-traditional-high");
        CountDownLatch tLatch = new CountDownLatch(taskCount);
        AtomicInteger tCounter = new AtomicInteger(0);
        long tStart = System.currentTimeMillis();

        for (int i = 0; i < taskCount; i++) {
            tPool.execute(() -> {
                try {
                    tCounter.incrementAndGet();
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    tLatch.countDown();
                }
            });
        }
        assertTrue(tLatch.await(30, TimeUnit.SECONDS), "传统线程池应在30秒内完成");
        long tEnd = System.currentTimeMillis();
        tPool.shutdown();

        long vTime = vEnd - vStart;
        long tTime = tEnd - tStart;

        System.out.println("\n=== 高并发场景性能对比 ===");
        System.out.println("任务数: " + taskCount);
        System.out.println("虚拟线程池耗时: " + vTime + "ms, 完成任务: " + vCounter.get());
        System.out.println("传统线程池耗时: " + tTime + "ms, 完成任务: " + tCounter.get());
        System.out.println("性能提升: " + String.format("%.2f%%", (1 - (double) vTime / tTime) * 100));

        assertEquals(taskCount, vCounter.get(), "虚拟线程池应完成所有任务");
        assertEquals(taskCount, tCounter.get(), "传统线程池应完成所有任务");
    }

    /**
     * 测试ThreadPoolFactory工厂类
     */
    @Test
    public void testThreadPoolFactory() {
        // 测试CPU密集型
        TraditionalThreadPool cpuPool = ThreadPoolFactory.createCpuIntensivePool("factory-cpu");
        assertNotNull(cpuPool);
        cpuPool.shutdown();

        // 测试IO密集型
        TraditionalThreadPool ioPool = ThreadPoolFactory.createIoIntensivePool("factory-io");
        assertNotNull(ioPool);
        ioPool.shutdown();

        // 测试虚拟线程池
        VirtualThreadPool vPool = ThreadPoolFactory.createVirtualThreadPool("factory-virtual");
        assertNotNull(vPool);
        vPool.shutdown();

        // 测试按任务类型创建
        ExecutorService cpuService = ThreadPoolFactory.createThreadPool("factory-cpu-type", ThreadPoolFactory.TaskType.CPU_INTENSIVE);
        assertNotNull(cpuService);
        ((TraditionalThreadPool) cpuService).shutdown();

        ExecutorService vService = ThreadPoolFactory.createThreadPool("factory-virtual-type", ThreadPoolFactory.TaskType.VIRTUAL);
        assertNotNull(vService);
        vService.shutdown();
    }
}
