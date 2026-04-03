package com.bootcloud.jvm;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JVM GC 调优工具类
 * 功能：获取GC统计信息、检测当前使用的垃圾回收器、输出GC调优建议
 * 适用场景：Java应用性能监控、JVM内存与GC问题排查、自动生成调优参数建议
 */
public class GCTuner {

    /**
     * 垃圾回收器MXBean列表（JVM启动时一次性获取，全局复用）
     * 用于获取所有GC实例的回收次数、回收时间等运行时数据
     */
    private static final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

    /**
     * 获取GC统计数据
     * @return 包含各回收器详情、总回收次数、总耗时、平均单次耗时的Map
     */
    public static Map<String, Object> getGCStatistics() {
        // 存储最终GC统计结果
        Map<String, Object> stats = new HashMap<>();
        // JVM进程总GC次数
        long totalCollections = 0;
        // JVM进程总GC耗时（毫秒）
        long totalTime = 0;

        // 遍历所有垃圾回收器，分别统计数据
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            // 垃圾回收器名称（如：G1 Young Generation、G1 Old Generation）
            String name = gcBean.getName();
            // 当前回收器总回收次数
            long count = gcBean.getCollectionCount();
            // 当前回收器总回收耗时(ms)
            long time = gcBean.getCollectionTime();

            // 单个GC回收器的详细数据
            Map<String, Long> gcStats = new HashMap<>();
            gcStats.put("count", count);
            gcStats.put("time", time);
            // 按回收器名称存入总统计Map
            stats.put(name, gcStats);

            // 累加全局总次数与总耗时
            totalCollections += count;
            totalTime += time;
        }

        // 放入全局汇总统计数据
        stats.put("totalCollections", totalCollections);
        stats.put("totalTime", totalTime);
        // 计算平均单次GC耗时（避免除0异常）
        stats.put("avgTimePerCollection", totalCollections > 0 ? (double) totalTime / totalCollections : 0);

        return stats;
    }

    /**
     * 打印GC调优建议
     * 逻辑：根据堆内存使用率、GC次数、GC耗时自动输出优化建议与JVM参数
     */
    public static void printGCRecommendations() {
        // 获取GC统计信息
        Map<String, Object> stats = getGCStatistics();
        System.out.println("=== GC Tuning Recommendations ===");
        System.out.println("Current GC Statistics:");
        System.out.println("  Total Collections: " + stats.get("totalCollections"));
        System.out.println("  Total GC Time: " + stats.get("totalTime") + "ms");
        System.out.println("  Avg Time per Collection: " + String.format("%.2fms", (Double) stats.get("avgTimePerCollection")));

        // 获取堆内存使用信息（依赖外部JVMInfo工具类）
        Map<String, Object> memoryInfo = JVMInfo.getMemoryInfo();
        double heapUsagePercent = (Double) memoryInfo.get("heap.usagePercent");

        System.out.println("\nHeap Usage: " + String.format("%.2f%%", heapUsagePercent));

        System.out.println("\nGC Tuning Recommendations:");

        // 堆内存使用率 > 80%：内存不足，建议增大堆
        if (heapUsagePercent > 80) {
            System.out.println("  [WARNING] Heap usage is high (>80%)");
            System.out.println("  Recommendation: Increase heap size with -Xmx parameter");
            System.out.println("  Example: -Xmx2g for 2GB heap");
        }
        // 堆内存使用率 60%~80%：中等，建议持续监控
        else if (heapUsagePercent > 60) {
            System.out.println("  [INFO] Heap usage is moderate (>60%)");
            System.out.println("  Recommendation: Monitor memory usage and consider increasing heap if needed");
        }

        // GC次数>1000 且 总耗时>10秒：GC过于频繁，建议更换低延迟回收器
        if ((Long) stats.get("totalCollections") > 1000 && (Long) stats.get("totalTime") > 10000) {
            System.out.println("  [WARNING] High GC activity detected");
            System.out.println("  Recommendation: Consider using G1GC for better pause time control");
            System.out.println("  Example: -XX:+UseG1GC");
            System.out.println("  Alternative: Use ZGC for ultra-low pause times (JDK 11+)");
            System.out.println("  Example: -XX:+UnlockExperimentalVMOptions -XX:+UseZGC");
        }

        // 平均单次GC停顿>100ms：停顿过长，建议使用G1并设置最大停顿时间
        if ((Double) stats.get("avgTimePerCollection") > 100) {
            System.out.println("  [WARNING] High average GC pause time (>100ms)");
            System.out.println("  Recommendation: Use G1GC with pause time goal");
            System.out.println("  Example: -XX:+UseG1GC -XX:MaxGCPauseMillis=200");
        }

        // 输出G1GC推荐配置（通用生产环境）
        System.out.println("\nG1GC Recommended Settings:");
        System.out.println("  -XX:+UseG1GC");
        System.out.println("  -XX:MaxGCPauseMillis=200");
        System.out.println("  -XX:G1HeapRegionSize=16m");
        System.out.println("  -XX:InitiatingHeapOccupancyPercent=45");

        // 输出ZGC推荐配置（JDK11+，超低延迟）
        System.out.println("\nZGC Recommended Settings (JDK 11+):");
        System.out.println("  -XX:+UnlockExperimentalVMOptions");
        System.out.println("  -XX:+UseZGC");
        System.out.println("  -XX:ZCollectionInterval=5");

        // 输出通用JVM内存参数说明
        System.out.println("\nGeneral JVM Memory Settings:");
        System.out.println("  -Xms<heap_size>  : Initial heap size");
        System.out.println("  -Xmx<heap_size>  : Maximum heap size");
        System.out.println("  -XX:NewSize=<size> : Young generation size");
        System.out.println("  -XX:MaxNewSize=<size> : Maximum young generation size");
        System.out.println("  -XX:MetaspaceSize=<size> : Initial metaspace size");
        System.out.println("  -XX:MaxMetaspaceSize=<size> : Maximum metaspace size");

        System.out.println("==================================");
    }

    /**
     * 检测当前JVM使用的垃圾回收器算法
     * Garbage Collection
     * @return 回收器名称：G1GC/ZGC/ParallelGC/CMS/SerialGC/Unknown
     * 现在常用G1,ZGC,CMS
     */
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