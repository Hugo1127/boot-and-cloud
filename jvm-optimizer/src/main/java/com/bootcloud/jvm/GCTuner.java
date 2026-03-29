package com.bootcloud.jvm;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GCTuner {
    private static final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

    public static Map<String, Object> getGCStatistics() {
        Map<String, Object> stats = new HashMap<>();
        long totalCollections = 0;
        long totalTime = 0;

        for (GarbageCollectorMXBean gcBean : gcBeans) {
            String name = gcBean.getName();
            long count = gcBean.getCollectionCount();
            long time = gcBean.getCollectionTime();

            Map<String, Long> gcStats = new HashMap<>();
            gcStats.put("count", count);
            gcStats.put("time", time);
            stats.put(name, gcStats);

            totalCollections += count;
            totalTime += time;
        }

        stats.put("totalCollections", totalCollections);
        stats.put("totalTime", totalTime);
        stats.put("avgTimePerCollection", totalCollections > 0 ? (double) totalTime / totalCollections : 0);

        return stats;
    }

    public static void printGCRecommendations() {
        Map<String, Object> stats = getGCStatistics();
        System.out.println("=== GC Tuning Recommendations ===");
        System.out.println("Current GC Statistics:");
        System.out.println("  Total Collections: " + stats.get("totalCollections"));
        System.out.println("  Total GC Time: " + stats.get("totalTime") + "ms");
        System.out.println("  Avg Time per Collection: " + String.format("%.2fms", (Double) stats.get("avgTimePerCollection")));

        Map<String, Object> memoryInfo = JVMInfo.getMemoryInfo();
        double heapUsagePercent = (Double) memoryInfo.get("heap.usagePercent");

        System.out.println("\nHeap Usage: " + String.format("%.2f%%", heapUsagePercent));

        System.out.println("\nGC Tuning Recommendations:");

        if (heapUsagePercent > 80) {
            System.out.println("  [WARNING] Heap usage is high (>80%)");
            System.out.println("  Recommendation: Increase heap size with -Xmx parameter");
            System.out.println("  Example: -Xmx2g for 2GB heap");
        } else if (heapUsagePercent > 60) {
            System.out.println("  [INFO] Heap usage is moderate (>60%)");
            System.out.println("  Recommendation: Monitor memory usage and consider increasing heap if needed");
        }

        if ((Long) stats.get("totalCollections") > 1000 && (Long) stats.get("totalTime") > 10000) {
            System.out.println("  [WARNING] High GC activity detected");
            System.out.println("  Recommendation: Consider using G1GC for better pause time control");
            System.out.println("  Example: -XX:+UseG1GC");
            System.out.println("  Alternative: Use ZGC for ultra-low pause times (JDK 11+)");
            System.out.println("  Example: -XX:+UnlockExperimentalVMOptions -XX:+UseZGC");
        }

        if ((Double) stats.get("avgTimePerCollection") > 100) {
            System.out.println("  [WARNING] High average GC pause time (>100ms)");
            System.out.println("  Recommendation: Use G1GC with pause time goal");
            System.out.println("  Example: -XX:+UseG1GC -XX:MaxGCPauseMillis=200");
        }

        System.out.println("\nG1GC Recommended Settings:");
        System.out.println("  -XX:+UseG1GC");
        System.out.println("  -XX:MaxGCPauseMillis=200");
        System.out.println("  -XX:G1HeapRegionSize=16m");
        System.out.println("  -XX:InitiatingHeapOccupancyPercent=45");

        System.out.println("\nZGC Recommended Settings (JDK 11+):");
        System.out.println("  -XX:+UnlockExperimentalVMOptions");
        System.out.println("  -XX:+UseZGC");
        System.out.println("  -XX:ZCollectionInterval=5");

        System.out.println("\nGeneral JVM Memory Settings:");
        System.out.println("  -Xms<heap_size>  : Initial heap size");
        System.out.println("  -Xmx<heap_size>  : Maximum heap size");
        System.out.println("  -XX:NewSize=<size> : Young generation size");
        System.out.println("  -XX:MaxNewSize=<size> : Maximum young generation size");
        System.out.println("  -XX:MetaspaceSize=<size> : Initial metaspace size");
        System.out.println("  -XX:MaxMetaspaceSize=<size> : Maximum metaspace size");

        System.out.println("==================================");
    }

    public static String detectGCAlgorithm() {
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            String name = gcBean.getName().toLowerCase();
            if (name.contains("g1")) {
                return "G1GC";
            } else if (name.contains("zgc")) {
                return "ZGC";
            } else if (name.contains("parallel")) {
                return "ParallelGC";
            } else if (name.contains("cms")) {
                return "CMS";
            } else if (name.contains("serial")) {
                return "SerialGC";
            }
        }
        return "Unknown";
    }
}
