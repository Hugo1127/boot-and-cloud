package com.bootcloud.jvm;

import java.lang.management.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class JVMProfiler {
    private static final long DEFAULT_INTERVAL_MS = 1000;
    private final long intervalMs;
    private ScheduledExecutorService scheduler;
    private volatile boolean running = false;

    public JVMProfiler() {
        this(DEFAULT_INTERVAL_MS);
    }

    public JVMProfiler(long intervalMs) {
        this.intervalMs = intervalMs;
    }

    public void start() {
        if (running) {
            return;
        }

        running = true;
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::profile, 0, intervalMs, TimeUnit.MILLISECONDS);
        System.out.println("JVM Profiler started with interval: " + intervalMs + "ms");
    }

    public void stop() {
        if (!running) {
            return;
        }

        running = false;
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("JVM Profiler stopped");
    }

    private void profile() {
        try {
            System.out.println("=== JVM Profiler Report ===");
            System.out.println("Timestamp: " + System.currentTimeMillis());
            
            System.out.println("\nMemory:");
            var memoryInfo = JVMInfo.getMemoryInfo();
            System.out.println("  Heap Used: " + formatSize((Long) memoryInfo.get("heap.used")));
            System.out.println("  Heap Max: " + formatSize((Long) memoryInfo.get("heap.max")));
            System.out.println("  Heap Usage: " + String.format("%.2f%%", (Double) memoryInfo.get("heap.usagePercent")));
            
            System.out.println("\nThreads:");
            var threadInfo = JVMInfo.getThreadInfo();
            System.out.println("  Thread Count: " + threadInfo.get("threadCount"));
            System.out.println("  Peak Thread Count: " + threadInfo.get("peakThreadCount"));
            
            System.out.println("\nClasses:");
            var classInfo = JVMInfo.getClassInfo();
            System.out.println("  Loaded Classes: " + classInfo.get("loadedClassCount"));
            System.out.println("  Total Loaded: " + classInfo.get("totalLoadedClassCount"));
            
            System.out.println("\nGC:");
            var gcInfo = JVMInfo.getGCInfo();
            if (gcInfo.get("gcName") != null) {
                System.out.println("  GC Name: " + gcInfo.get("gcName"));
                System.out.println("  GC Count: " + gcInfo.get("gcCollectionCount"));
                System.out.println("  GC Time: " + gcInfo.get("gcCollectionTime") + "ms");
            }
            
            System.out.println("========================\n");
        } catch (Exception e) {
            System.err.println("Error profiling JVM: " + e.getMessage());
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
