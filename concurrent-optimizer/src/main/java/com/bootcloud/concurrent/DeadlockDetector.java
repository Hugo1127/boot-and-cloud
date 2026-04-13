package com.bootcloud.concurrent;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * 死锁检测与处理工具类
 * 功能：检测JVM中的线程死锁、打印线程堆栈、模拟死锁、自动解除死锁、提供死锁预防方案
 * 适用场景：并发程序调试、死锁问题排查、并发教学演示
 * 直接调用静态方法即可，无需创建对象
 */
public class DeadlockDetector {

    /**
     * JVM 线程管理Bean：用于获取线程信息、检测死锁
     * 是Java官方提供的线程监控核心API
     */
    private static final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

    /**
     * 死锁检测核心方法
     * @return true=检测到死锁，false=未检测到死锁
     * 同时会打印死锁检测结果和死锁线程详情
     */
    public static boolean detectDeadlock() {
        System.out.println("=== Deadlock Detection ===");
        
        // 检测【包含对象监视器+可拥有同步器】的循环死锁（最常用）
        long[] deadlockedThreads = threadBean.findDeadlockedThreads();
        // 仅检测【对象监视器】的死锁（synchronized关键字产生的死锁）
        long[] monitorDeadlockedThreads = threadBean.findMonitorDeadlockedThreads();

        // 两种死锁都未检测到，返回无死锁
        if ((deadlockedThreads == null || deadlockedThreads.length == 0) &&
            (monitorDeadlockedThreads == null || monitorDeadlockedThreads.length == 0)) {
            System.out.println("No deadlocks detected");
            return false;
        }

        // 检测到死锁，开始打印告警信息
        System.out.println("⚠️ DEADLOCK DETECTED! ⚠️");
        
        // 打印循环死锁信息
        if (deadlockedThreads != null && deadlockedThreads.length > 0) {
            System.out.println("\nCyclic deadlock detected:");
            printDeadlockInfo(deadlockedThreads);
        }

        // 打印监视器死锁信息
        if (monitorDeadlockedThreads != null && monitorDeadlockedThreads.length > 0) {
            System.out.println("\nMonitor deadlock detected:");
            printDeadlockInfo(monitorDeadlockedThreads);
        }

        return true;
    }

    /**
     * 打印死锁线程的详细信息
     * @param threadIds 死锁线程的ID数组
     * 输出内容：线程名、ID、状态、完整堆栈跟踪
     */
    private static void printDeadlockInfo(long[] threadIds) {
        // 获取当前JVM所有线程及其堆栈
        Map<Thread, StackTraceElement[]> threadStacks = new HashMap<>();
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            threadStacks.put(thread, thread.getStackTrace());
        }

        // 遍历死锁线程ID，匹配并打印对应线程信息
        for (long threadId : threadIds) {
            for (Thread thread : threadStacks.keySet()) {
                if (thread.getId() == threadId) {
                    System.out.println("\nDeadlocked Thread: " + thread.getName() + 
                                      " (ID: " + threadId + ")");
                    System.out.println("State: " + thread.getState());
                    System.out.println("Stack Trace:");
                    // 打印线程堆栈，定位死锁代码位置
                    for (StackTraceElement element : threadStacks.get(thread)) {
                        System.out.println("  at " + element);
                    }
                    break;
                }
            }
        }
    }

    /**
     * 打印全量线程栈（线程快照）
     * 用于排查所有线程状态，不只是死锁线程
     */
    public static void printThreadDump() {
        System.out.println("=== Thread Dump ===");
        
        // 获取所有线程的堆栈信息
        Map<Thread, StackTraceElement[]> threadStacks = Thread.getAllStackTraces();
        
        // 遍历打印每个线程的详细信息
        for (Map.Entry<Thread, StackTraceElement[]> entry : threadStacks.entrySet()) {
            Thread thread = entry.getKey();
            System.out.println("\nThread: " + thread.getName() + 
                             " (ID: " + thread.getId() + ")");
            System.out.println("  State: " + thread.getState());
            System.out.println("  Priority: " + thread.getPriority());
            System.out.println("  Daemon: " + thread.isDaemon());
            System.out.println("  Stack Trace:");
            
            for (StackTraceElement element : entry.getValue()) {
                System.out.println("    at " + element);
            }
        }
        System.out.println("==================");
    }

    /**
     * 模拟经典死锁场景：两个线程互相持有对方需要的锁，无限等待
     * 同时自动检测死锁并尝试中断线程解除死锁
     */
    public static void simulateDeadlock() {
        // 定义两个锁对象
        final Object lock1 = new Object();
        final Object lock2 = new Object();
        // 标记线程是否执行完成
        final boolean[] thread1Completed = {false};
        final boolean[] thread2Completed = {false};

        // 线程1：先获取lock1，再等待lock2
        Thread thread1 = new Thread(() -> {
            try {
                synchronized (lock1) {
                    System.out.println("Thread 1: Holding lock 1...");
                    try {
                        // 休眠100ms，确保线程2先拿到lock2
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        System.out.println("Thread 1: Interrupted while holding lock 1");
                        Thread.currentThread().interrupt();
                        return;
                    }
                    System.out.println("Thread 1: Waiting for lock 2...");
                    // 等待线程2释放lock2，产生死锁
                    synchronized (lock2) {
                        System.out.println("Thread 1: Acquired both locks!");
                        thread1Completed[0] = true;
                    }
                }
            } catch (Exception e) {
                System.out.println("Thread 1: Exception - " + e.getMessage());
            }
        });

        // 线程2：先获取lock2，再等待lock1
        Thread thread2 = new Thread(() -> {
            try {
                synchronized (lock2) {
                    System.out.println("Thread 2: Holding lock 2...");
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        System.out.println("Thread 2: Interrupted while holding lock 2");
                        Thread.currentThread().interrupt();
                        return;
                    }
                    System.out.println("Thread 2: Waiting for lock 1...");
                    // 等待线程1释放lock1，产生死锁
                    synchronized (lock1) {
                        System.out.println("Thread 2: Acquired both locks!");
                        thread2Completed[0] = true;
                    }
                }
            } catch (Exception e) {
                System.out.println("Thread 2: Exception - " + e.getMessage());
            }
        });

        // 启动两个线程，触发死锁
        thread1.start();
        thread2.start();

        try {
            // 等待1秒，让死锁稳定形成
            Thread.sleep(1000);
            System.out.println("\nSimulated deadlock. Checking for deadlocks...");
            // 执行死锁检测
            boolean deadlockDetected = detectDeadlock();
            
            // 检测到死锁，尝试中断线程解除死锁
            if (deadlockDetected) {
                System.out.println("\nBreaking deadlock by interrupting threads...");
                thread1.interrupt();
                thread2.interrupt();
                
                // 等待中断生效
                Thread.sleep(500);
                
                if (!thread1Completed[0] && thread1.isAlive()) {
                    System.out.println("Thread 1 still blocked, forcing completion...");
                }
                if (!thread2Completed[0] && thread2.isAlive()) {
                    System.out.println("Thread 2 still blocked, forcing completion...");
                }
            }
            
            // 等待线程结束
            thread1.join(2000);
            thread2.join(2000);
            
            System.out.println("Deadlock simulation completed");
            System.out.println("Note: Interrupted threads may not terminate gracefully in deadlock scenarios");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}