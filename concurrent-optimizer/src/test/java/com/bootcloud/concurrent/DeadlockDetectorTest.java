package com.bootcloud.concurrent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DeadlockDetectorTest {

    @Test
    public void testDetectNoDeadlock() {
        assertFalse(DeadlockDetector.detectDeadlock());
    }

    @Test
    public void testSimulateDeadlock() {
        assertDoesNotThrow(() -> DeadlockDetector.simulateDeadlock());
    }

    @Test
    public void testPrintThreadDump() {
        assertDoesNotThrow(() -> DeadlockDetector.printThreadDump());
    }

    @Test
    public void testPrintDeadlockPreventionTips() {
        assertDoesNotThrow(() -> DeadlockDetector.printDeadlockPreventionTips());
    }
}
