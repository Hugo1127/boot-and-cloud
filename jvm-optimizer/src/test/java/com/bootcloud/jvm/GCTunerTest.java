package com.bootcloud.jvm;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class GCTunerTest {

    @Test
    public void testGetGCStatistics() {
        Map<String, Object> stats = GCTuner.getGCStatistics();
        assertNotNull(stats);
        assertTrue(stats.containsKey("totalCollections"));
        assertTrue(stats.containsKey("totalTime"));
        assertTrue(stats.containsKey("avgTimePerCollection"));
        
        assertTrue((Long) stats.get("totalCollections") >= 0);
        assertTrue((Long) stats.get("totalTime") >= 0);
        
        System.out.println("GC Statistics: " + stats);
    }

    @Test
    public void testPrintGCRecommendations() {
        assertDoesNotThrow(() -> GCTuner.printGCRecommendations());
    }

    @Test
    public void testDetectGCAlgorithm() {
        String gcAlgorithm = GCTuner.detectGCAlgorithm();
        assertNotNull(gcAlgorithm);
        assertNotEquals("Unknown", gcAlgorithm);
        System.out.println("Detected GC Algorithm: " + gcAlgorithm);
    }
}
