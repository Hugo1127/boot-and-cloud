package com.bootcloud.jvm;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class JVMInfoTest {

    @Test
    public void testGetMemoryInfo() {
        Map<String, Object> memoryInfo = JVMInfo.getMemoryInfo();
        assertNotNull(memoryInfo);
        assertTrue(memoryInfo.containsKey("heap.used"));
        assertTrue(memoryInfo.containsKey("heap.max"));
        assertTrue(memoryInfo.containsKey("heap.usagePercent"));
        
        assertTrue((Long) memoryInfo.get("heap.max") > 0);
        assertTrue((Double) memoryInfo.get("heap.usagePercent") >= 0);
        assertTrue((Double) memoryInfo.get("heap.usagePercent") <= 100);
        
        System.out.println("Memory Info: " + memoryInfo);
    }

    @Test
    public void testGetThreadInfo() {
        Map<String, Object> threadInfo = JVMInfo.getThreadInfo();
        assertNotNull(threadInfo);
        assertTrue(threadInfo.containsKey("threadCount"));
        assertTrue(threadInfo.containsKey("peakThreadCount"));
        
        assertTrue((Integer) threadInfo.get("threadCount") > 0);
        assertTrue((Integer) threadInfo.get("peakThreadCount") >= (Integer) threadInfo.get("threadCount"));
        
        System.out.println("Thread Info: " + threadInfo);
    }

    @Test
    public void testGetClassInfo() {
        Map<String, Object> classInfo = JVMInfo.getClassInfo();
        assertNotNull(classInfo);
        assertTrue(classInfo.containsKey("loadedClassCount"));
        assertTrue(classInfo.containsKey("totalLoadedClassCount"));
        
        // 兼容 Integer 和 Long 类型
        Object loadedCount = classInfo.get("loadedClassCount");
        Object totalLoadedCount = classInfo.get("totalLoadedClassCount");
        
        if (loadedCount instanceof Integer) {
            assertTrue((Integer) loadedCount > 0);
        } else {
            assertTrue((Long) loadedCount > 0);
        }
        
        if (totalLoadedCount instanceof Integer) {
            assertTrue((Integer) totalLoadedCount > 0);
        } else {
            assertTrue((Long) totalLoadedCount > 0);
        }
        
        System.out.println("Class Info: " + classInfo);
    }

    @Test
    public void testGetGCInfo() {
        Map<String, Object> gcInfo = JVMInfo.getGCInfo();
        assertNotNull(gcInfo);
        
        if (gcInfo.containsKey("gcName")) {
            assertNotNull(gcInfo.get("gcName"));
            assertTrue((Long) gcInfo.get("gcCollectionCount") >= 0);
            assertTrue((Long) gcInfo.get("gcCollectionTime") >= 0);
        }
        
        System.out.println("GC Info: " + gcInfo);
    }

    @Test
    public void testGetRuntimeInfo() {
        Map<String, Object> runtimeInfo = JVMInfo.getRuntimeInfo();
        assertNotNull(runtimeInfo);
        assertTrue(runtimeInfo.containsKey("jvmName"));
        assertTrue(runtimeInfo.containsKey("jvmVersion"));
        assertTrue(runtimeInfo.containsKey("uptime"));
        
        assertNotNull(runtimeInfo.get("jvmName"));
        assertTrue((Long) runtimeInfo.get("uptime") > 0);
        
        System.out.println("Runtime Info: " + runtimeInfo);
    }

    @Test
    public void testPrintAllInfo() {
        assertDoesNotThrow(() -> JVMInfo.printAllInfo());
    }
}
