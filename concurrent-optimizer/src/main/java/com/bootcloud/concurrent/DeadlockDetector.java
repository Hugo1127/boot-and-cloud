package com.bootcloud.concurrent;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

public class DeadlockDetector {
    private static final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

    public static boolean detectDeadlock() {
        System.out.println("=== Deadlock Detection ===");
        
        long[] deadlockedThreads = threadBean.findDeadlockedThreads();
        long[] monitorDeadlockedThreads = threadBean.findMonitorDeadlockedThreads();

        if ((deadlockedThreads == null || deadlockedThreads.length == 0) &&
            (monitorDeadlockedThreads == null || monitorDeadlockedThreads.length == 0)) {
            System.out.println("No deadlocks detected");
            return false;
        }

        System.out.println("⚠️ DEADLOCK DETECTED! ⚠️");
        
        if (deadlockedThreads != null && deadlockedThreads.length > 0) {
            System.out.println("\nCyclic deadlock detected:");
            printDeadlockInfo(deadlockedThreads);
        }

        if (monitorDeadlockedThreads != null && monitorDeadlockedThreads.length > 0) {
            System.out.println("\nMonitor deadlock detected:");
            printDeadlockInfo(monitorDeadlockedThreads);
        }

        return true;
    }

    private static void printDeadlockInfo(long[] threadIds) {
        Map<Thread, StackTraceElement[]> threadStacks = new HashMap<>();
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            threadStacks.put(thread, thread.getStackTrace());
        }

        for (long threadId : threadIds) {
            for (Thread thread : threadStacks.keySet()) {
                if (thread.getId() == threadId) {
                    System.out.println("\nDeadlocked Thread: " + thread.getName() + 
                                      " (ID: " + threadId + ")");
                    System.out.println("State: " + thread.getState());
                    System.out.println("Stack Trace:");
                    for (StackTraceElement element : threadStacks.get(thread)) {
                        System.out.println("  at " + element);
                    }
                    break;
                }
            }
        }
    }

    public static void printThreadDump() {
        System.out.println("=== Thread Dump ===");
        
        Map<Thread, StackTraceElement[]> threadStacks = Thread.getAllStackTraces();
        
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

    public static void simulateDeadlock() {
        final Object lock1 = new Object();
        final Object lock2 = new Object();
        final boolean[] thread1Completed = {false};
        final boolean[] thread2Completed = {false};

        Thread thread1 = new Thread(() -> {
            try {
                synchronized (lock1) {
                    System.out.println("Thread 1: Holding lock 1...");
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        System.out.println("Thread 1: Interrupted while holding lock 1");
                        Thread.currentThread().interrupt();
                        return;
                    }
                    System.out.println("Thread 1: Waiting for lock 2...");
                    synchronized (lock2) {
                        System.out.println("Thread 1: Acquired both locks!");
                        thread1Completed[0] = true;
                    }
                }
            } catch (Exception e) {
                System.out.println("Thread 1: Exception - " + e.getMessage());
            }
        });

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
                    synchronized (lock1) {
                        System.out.println("Thread 2: Acquired both locks!");
                        thread2Completed[0] = true;
                    }
                }
            } catch (Exception e) {
                System.out.println("Thread 2: Exception - " + e.getMessage());
            }
        });

        thread1.start();
        thread2.start();

        try {
            Thread.sleep(1000);
            System.out.println("\nSimulated deadlock. Checking for deadlocks...");
            boolean deadlockDetected = detectDeadlock();
            
            if (deadlockDetected) {
                System.out.println("\nBreaking deadlock by interrupting threads...");
                thread1.interrupt();
                thread2.interrupt();
                
                Thread.sleep(500);
                
                if (!thread1Completed[0] && thread1.isAlive()) {
                    System.out.println("Thread 1 still blocked, forcing completion...");
                }
                if (!thread2Completed[0] && thread2.isAlive()) {
                    System.out.println("Thread 2 still blocked, forcing completion...");
                }
            }
            
            thread1.join(2000);
            thread2.join(2000);
            
            System.out.println("Deadlock simulation completed");
            System.out.println("Note: Interrupted threads may not terminate gracefully in deadlock scenarios");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void printDeadlockPreventionTips() {
        System.out.println("=== Deadlock Prevention Tips ===");
        System.out.println("1. Lock Ordering:");
        System.out.println("   - Always acquire locks in the same order");
        System.out.println("   - Use a global lock ordering strategy");
        
        System.out.println("\n2. Lock Timeout:");
        System.out.println("   - Use tryLock() with timeout instead of lock()");
        System.out.println("   - Implement retry logic with backoff");
        
        System.out.println("\n3. Avoid Nested Locks:");
        System.out.println("   - Minimize the number of locks held simultaneously");
        System.out.println("   - Release locks as soon as possible");
        
        System.out.println("\n4. Use Higher-Level Concurrency Utilities:");
        System.out.println("   - ConcurrentHashMap instead of synchronized HashMap");
        System.out.println("   - AtomicInteger instead of synchronized counters");
        System.out.println("   - ConcurrentLinkedQueue instead of synchronized Queue");
        
        System.out.println("\n5. Deadlock Detection:");
        System.out.println("   - Enable deadlock detection in development");
        System.out.println("   - Monitor thread states regularly");
        System.out.println("   - Use jstack to analyze deadlocks");
        System.out.println("   - Example: jstack <pid>");
        
        System.out.println("\n6. Resource Hierarchy:");
        System.out.println("   - Assign numerical ordering to resources");
        System.out.println("   - Always request resources in ascending order");
        
        System.out.println("\n7. Livelock Prevention:");
        System.out.println("   - Add random delays between retries");
        System.out.println("   - Use exponential backoff");
        
        System.out.println("================================");
    }
}
