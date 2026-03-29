package com.bootcloud.jvm;

import java.lang.management.*;
import java.util.HashMap;
import java.util.Map;

public class JVMInfo {
    private static final Runtime runtime = Runtime.getRuntime();
    private static final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private static final MemoryPoolMXBean heapPoolMXBean = findPool("Heap");
    private static final MemoryPoolMXBean nonHeapPoolMXBean = findPool("Non-heap");
    private static final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private static final ClassLoadingMXBean classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();
    private static final CompilationMXBean compilationMXBean = ManagementFactory.getCompilationMXBean();
    private static final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
    private static final GarbageCollectorMXBean gcMXBean = findGCMXBean();

    private static MemoryPoolMXBean findPool(String type) {
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getType().name().equals(type)) {
                return pool;
            }
        }
        return null;
    }

    private static GarbageCollectorMXBean findGCMXBean() {
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            return gc;
        }
        return null;
    }

    public static Map<String, Object> getMemoryInfo() {
        Map<String, Object> memoryInfo = new HashMap<>();
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();

        memoryInfo.put("heap.used", heapUsage.getUsed());
        memoryInfo.put("heap.committed", heapUsage.getCommitted());
        memoryInfo.put("heap.max", heapUsage.getMax());
        memoryInfo.put("heap.usagePercent", 
            heapUsage.getMax() > 0 ? (heapUsage.getUsed() * 100.0 / heapUsage.getMax()) : 0);

        memoryInfo.put("nonHeap.used", nonHeapUsage.getUsed());
        memoryInfo.put("nonHeap.committed", nonHeapUsage.getCommitted());
        memoryInfo.put("nonHeap.max", nonHeapUsage.getMax());

        memoryInfo.put("jvm.maxMemory", runtime.maxMemory());
        memoryInfo.put("jvm.totalMemory", runtime.totalMemory());
        memoryInfo.put("jvm.freeMemory", runtime.freeMemory());

        return memoryInfo;
    }

    public static Map<String, Object> getThreadInfo() {
        Map<String, Object> threadInfo = new HashMap<>();
        threadInfo.put("threadCount", threadMXBean.getThreadCount());
        threadInfo.put("peakThreadCount", threadMXBean.getPeakThreadCount());
        threadInfo.put("daemonThreadCount", threadMXBean.getDaemonThreadCount());
        threadInfo.put("totalStartedThreadCount", threadMXBean.getTotalStartedThreadCount());
        return threadInfo;
    }

    public static Map<String, Object> getClassInfo() {
        Map<String, Object> classInfo = new HashMap<>();
        classInfo.put("loadedClassCount", classLoadingMXBean.getLoadedClassCount());
        classInfo.put("totalLoadedClassCount", classLoadingMXBean.getTotalLoadedClassCount());
        classInfo.put("unloadedClassCount", classLoadingMXBean.getUnloadedClassCount());
        return classInfo;
    }

    public static Map<String, Object> getGCInfo() {
        Map<String, Object> gcInfo = new HashMap<>();
        if (gcMXBean != null) {
            gcInfo.put("gcName", gcMXBean.getName());
            gcInfo.put("gcCollectionCount", gcMXBean.getCollectionCount());
            gcInfo.put("gcCollectionTime", gcMXBean.getCollectionTime());
        }
        return gcInfo;
    }

    public static Map<String, Object> getRuntimeInfo() {
        Map<String, Object> runtimeInfo = new HashMap<>();
        runtimeInfo.put("jvmName", runtimeMXBean.getVmName());
        runtimeInfo.put("jvmVersion", runtimeMXBean.getVmVersion());
        runtimeInfo.put("jvmVendor", runtimeMXBean.getVmVendor());
        runtimeInfo.put("uptime", runtimeMXBean.getUptime());
        runtimeInfo.put("startTime", runtimeMXBean.getStartTime());
        runtimeInfo.put("systemProperties", runtimeMXBean.getSystemProperties());
        return runtimeInfo;
    }

    public static Map<String, Object> getCompilationInfo() {
        Map<String, Object> compilationInfo = new HashMap<>();
        if (compilationMXBean != null) {
            compilationInfo.put("compilerName", compilationMXBean.getName());
            compilationInfo.put("totalCompilationTime", compilationMXBean.getTotalCompilationTime());
            if (compilationMXBean.isCompilationTimeMonitoringSupported()) {
                compilationInfo.put("compilationTimeMonitoring", "supported");
            }
        }
        return compilationInfo;
    }

    public static void printAllInfo() {
        System.out.println("=== JVM Memory Info ===");
        System.out.println(getMemoryInfo());
        System.out.println("\n=== Thread Info ===");
        System.out.println(getThreadInfo());
        System.out.println("\n=== Class Info ===");
        System.out.println(getClassInfo());
        System.out.println("\n=== GC Info ===");
        System.out.println(getGCInfo());
        System.out.println("\n=== Runtime Info ===");
        System.out.println(getRuntimeInfo());
        System.out.println("\n=== Compilation Info ===");
        System.out.println(getCompilationInfo());
    }
}
